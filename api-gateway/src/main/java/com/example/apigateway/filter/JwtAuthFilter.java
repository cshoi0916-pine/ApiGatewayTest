package com.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.util.concurrent.TimeoutException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> ROLE_PRIORITY = List.of("ROLE_ADMIN", "ROLE_USER");

    private static final List<String> WHITELIST = List.of(
        "/auth/",
        "/public/",
        "/login.html",
        "/login",
        "/console",
        "/actuator",
        "/registry",
        "/swagger-ui",
        "/v3/api-docs",
        "/datamodel",
        "/css/",
        "/js/",
        "/test-dynamic/slow",   // 타임아웃 테스트용 (인증서버 없이 라우트 타임아웃 테스트)
        "/test-dynamic/flaky"   // 재시도 테스트용
    );

    @Value("${jwt.enabled:true}")
    private boolean enabled;

    private final WebClient webClient;
    private final String authServerUrl;

    public JwtAuthFilter(WebClient.Builder webClientBuilder,
                         @Value("${auth.server-url}") String authServerUrl) {
        this.webClient = webClientBuilder.build();
        this.authServerUrl = authServerUrl;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) return chain.filter(exchange);

        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) return chain.filter(exchange);

        if (Boolean.TRUE.equals(exchange.getAttributes().get("AUTHENTICATED_BY_API_KEY"))) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);
        if (token == null) {
            if (isBrowserRequest(exchange)) {
                return redirectToLogin(exchange);
            }
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다.");
        }

        return validateAndProceed(exchange, chain, token, path, false);
    }

    /**
     * 토큰 유효성 검증 후 다운스트림으로 전달.
     * afterRefresh=true이면 401 시 재갱신 시도를 하지 않아 무한루프를 방지한다.
     */
    private Mono<Void> validateAndProceed(ServerWebExchange exchange, GatewayFilterChain chain,
                                          String token, String path, boolean afterRefresh) {
        return webClient.get()
            .uri(authServerUrl + "/api/auth/validate")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(3))
            .flatMap(response -> {
                Map<String, Object> body = response.getBody();
                String userId = body != null ? (String) body.get("userId") : null;
                @SuppressWarnings("unchecked")
                List<String> roles = (body != null && body.get("roles") instanceof List<?>)
                    ? (List<String>) body.get("roles") : List.of();
                String role = extractHighestRole(roles);

                log.info("[JWT] 인증 성공 → X-User-Id={}, X-User-Role={}", userId, role);

                String finalUserId = userId != null ? userId : "";
                String finalRole   = role   != null ? role   : "";
                ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                        .headers(h -> {
                            h.remove("X-User-Id");
                            h.remove("X-User-Role");
                            h.remove("X-Dev-Bypass");
                            h.remove("X-Auth-Token");
                        })
                        .header("X-User-Id",    finalUserId)
                        .header("X-User-Role",  finalRole)
                        .header("X-Auth-Token", token)
                    )
                    .build();
                return chain.filter(mutated);
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                int code = e.getStatusCode().value();
                log.warn("[JWT] validate 거부: status={}, path={}", code, path);
                if (e.getStatusCode().is4xxClientError()) {
                    // 401이고 아직 refresh 미시도: refresh_token 쿠키로 재발급 시도
                    if (code == 401 && !afterRefresh) {
                        return tryRefreshAndProceed(exchange, chain, path);
                    }
                    if (code != 403 && isBrowserRequest(exchange)) {
                        return redirectToLogin(exchange);
                    }
                    String msg = switch (code) {
                        case 400 -> "잘못된 토큰 형식입니다.";
                        case 401 -> "토큰이 만료되었거나 로그아웃된 토큰입니다.";
                        case 403 -> "접근 권한이 없습니다.";
                        default  -> "유효하지 않은 토큰입니다.";
                    };
                    HttpStatus status = (code == 403) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
                    return writeError(exchange, status, msg);
                }
                log.error("[JWT] 인증서버 내부 오류: status={}, path={}", code, path);
                return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "인증 서버 내부 오류입니다. (" + code + ")");
            })
            .onErrorResume(ReadTimeoutException.class, e -> {
                log.error("[JWT] 인증서버 Read 타임아웃: path={}", path);
                return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "인증 서버가 응답하지 않습니다 (타임아웃).");
            })
            .onErrorResume(TimeoutException.class, e -> {
                log.error("[JWT] 인증서버 3초 타임아웃: path={}", path);
                return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "인증 서버가 응답하지 않습니다 (3초 타임아웃).");
            })
            .onErrorResume(e -> {
                log.error("[JWT] 인증서버 연결 실패: {}", e.getMessage());
                return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "인증 서버에 연결할 수 없습니다.");
            });
    }

    /**
     * refresh_token 쿠키로 새 access_token을 발급받아 원래 요청을 재시도한다.
     * refresh 자체가 실패하면 로그인 페이지로 리다이렉트한다.
     */
    private Mono<Void> tryRefreshAndProceed(ServerWebExchange exchange, GatewayFilterChain chain, String path) {
        HttpCookie refreshCookie = exchange.getRequest().getCookies().getFirst("refresh_token");
        if (refreshCookie == null) {
            log.info("[JWT] access_token 만료, refresh_token 쿠키 없음 → 로그인 리다이렉트: path={}", path);
            if (isBrowserRequest(exchange)) return redirectToLogin(exchange);
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해주세요.");
        }

        log.info("[JWT] access_token 만료 → refresh 시도: path={}", path);
        return webClient.post()
            .uri(authServerUrl + "/api/auth/refresh")
            .bodyValue(Map.of("refreshToken", refreshCookie.getValue()))
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(3))
            .flatMap(refreshResponse -> {
                Map<String, Object> body = refreshResponse.getBody();
                String newAccessToken = body != null ? (String) body.get("access_token") : null;
                if (newAccessToken == null) {
                    log.warn("[JWT] refresh 응답에 access_token 없음 → 로그인 리다이렉트");
                    if (isBrowserRequest(exchange)) return redirectToLogin(exchange);
                    return writeError(exchange, HttpStatus.UNAUTHORIZED, "토큰 갱신에 실패했습니다.");
                }

                log.info("[JWT] 토큰 갱신 성공 → 새 jwt_token 쿠키 발급");
                ResponseCookie newJwtCookie = ResponseCookie.from("jwt_token", newAccessToken)
                    .httpOnly(true).secure(true).path("/").maxAge(Duration.ofHours(1)).sameSite("Lax").build();
                exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, newJwtCookie.toString());

                // rotating refresh token 지원 — 새 refresh_token이 있으면 쿠키 갱신
                String newRefreshToken = body != null ? (String) body.get("refresh_token") : null;
                if (newRefreshToken != null) {
                    ResponseCookie newRefreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                        .httpOnly(true).secure(true).path("/").maxAge(Duration.ofDays(7)).sameSite("Lax").build();
                    exchange.getResponse().getHeaders().add(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());
                }

                return validateAndProceed(exchange, chain, newAccessToken, path, true);
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                log.warn("[JWT] refresh 거부({}): 로그인 리다이렉트", e.getStatusCode().value());
                if (isBrowserRequest(exchange)) return redirectToLogin(exchange);
                return writeError(exchange, HttpStatus.UNAUTHORIZED, "토큰 갱신에 실패했습니다. 다시 로그인해주세요.");
            })
            .onErrorResume(e -> {
                log.error("[JWT] refresh 서버 연결 실패: {}", e.getMessage());
                if (isBrowserRequest(exchange)) return redirectToLogin(exchange);
                return writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "인증 서버에 연결할 수 없습니다.");
            });
    }

    private String extractHighestRole(List<String> roles) {
        for (String priority : ROLE_PRIORITY) {
            if (roles.contains(priority)) return priority;
        }
        return roles.isEmpty() ? null : roles.get(0);
    }

    private String extractToken(ServerWebExchange exchange) {
        // 1. Authorization: Bearer 헤더 우선 (SPA, API 클라이언트)
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 2. jwt_token 쿠키 fallback (MPA 브라우저)
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("jwt_token");
        return cookie != null ? cookie.getValue() : null;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private boolean isBrowserRequest(ServerWebExchange exchange) {
        String accept = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCEPT);
        return accept != null && accept.contains("text/html");
    }

    private Mono<Void> redirectToLogin(ServerWebExchange exchange) {
        String path  = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        String originalUrl = query != null ? path + "?" + query : path;
        String loginUrl = "/login.html?redirect=" + URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);

        log.debug("[JWT] 브라우저 미인증 요청 → 로그인 리다이렉트: {}", originalUrl);
        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
        exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, loginUrl);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status":%d,"error":"%s","message":"%s"}
                """.formatted(status.value(), status.getReasonPhrase(), message);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -40;
    }
}
