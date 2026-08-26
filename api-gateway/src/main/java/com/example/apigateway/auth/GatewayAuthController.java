package com.example.apigateway.auth;

import com.example.apigateway.security.IpBlacklistEntity;
import com.example.apigateway.security.IpBlacklistFilter;
import com.example.apigateway.security.IpBlacklistRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 게이트웨이 자체에서 처리하는 인증 엔드포인트.
 * /auth/login, /auth/logout 요청을 실제 인증서버(auth.server-url)로 프록시한다.
 *
 * @RestController가 SCG 라우팅보다 우선 처리되므로, DB에 /auth/** 라우트가 있어도
 * 이 컨트롤러가 먼저 요청을 가로챈다.
 */
@RestController
@RequestMapping("/auth")
public class GatewayAuthController {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthController.class);

    private static final String SESSION_KEY_PREFIX = "gateway:session:";
    private static final Duration SESSION_TTL = Duration.ofHours(8);

    private static final String BRUTE_FORCE_KEY_PREFIX = "login:fail:";

    private final WebClient webClient;
    private final String authServerUrl;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final LoginLogRepository loginLogRepository;
    private final IpBlacklistRepository ipBlacklistRepository;
    private final IpBlacklistFilter ipBlacklistFilter;

    private final boolean bruteForceEnabled;
    private final int maxAttempts;
    private final int blockHours;
    private final int windowMinutes;

    public GatewayAuthController(WebClient.Builder webClientBuilder,
                                  @Value("${auth.server-url}") String authServerUrl,
                                  ReactiveStringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  LoginLogRepository loginLogRepository,
                                  IpBlacklistRepository ipBlacklistRepository,
                                  IpBlacklistFilter ipBlacklistFilter,
                                  @Value("${brute-force.enabled:true}") boolean bruteForceEnabled,
                                  @Value("${brute-force.max-attempts:5}") int maxAttempts,
                                  @Value("${brute-force.block-hours:1}") int blockHours,
                                  @Value("${brute-force.window-minutes:10}") int windowMinutes) {
        this.webClient = webClientBuilder.build();
        this.authServerUrl = authServerUrl;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.loginLogRepository = loginLogRepository;
        this.ipBlacklistRepository = ipBlacklistRepository;
        this.ipBlacklistFilter = ipBlacklistFilter;
        this.bruteForceEnabled = bruteForceEnabled;
        this.maxAttempts = maxAttempts;
        this.blockHours = blockHours;
        this.windowMinutes = windowMinutes;
    }

    /** 로그인 — { username, password } JSON → 인증서버 프록시 + jwt_token·session_id 쿠키 발급 */
    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(
            @RequestBody Map<String, String> body,
            ServerWebExchange exchange) {
        log.debug("[Auth] 로그인 요청 → {}", authServerUrl);
        String username = body.getOrDefault("username", "unknown");
        String clientIp = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null && exchange.getRequest().getRemoteAddress() != null) {
            clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        final String resolvedIp = clientIp != null ? clientIp : "unknown";

        return webClient.post()
            .uri(authServerUrl + "/api/auth/login")
            .bodyValue(body)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .<ResponseEntity<Map<String, Object>>>flatMap(entity -> {
                if (entity.getStatusCode().is2xxSuccessful() && entity.getBody() != null) {
                    String accessToken  = (String) entity.getBody().get("access_token");
                    String refreshToken = (String) entity.getBody().get("refresh_token");
                    if (accessToken != null) {
                        Map<String, String> sessionData = new HashMap<>();
                        sessionData.put("accessToken",  accessToken);
                        sessionData.put("refreshToken", refreshToken != null ? refreshToken : "");

                        return generateUniqueSessionId()
                            .flatMap(sessionId -> saveSession(sessionId, sessionData)
                                .then(Mono.fromCallable(() -> {
                                    ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", accessToken)
                                        .httpOnly(true).secure(true).path("/").maxAge(Duration.ofHours(1)).sameSite("Lax").build();
                                    ResponseCookie sessionCookie = ResponseCookie.from("session_id", sessionId)
                                        .httpOnly(true).secure(true).path("/").maxAge(SESSION_TTL).sameSite("Lax").build();

                                    var builder = ResponseEntity.status(entity.getStatusCode())
                                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                        .header(HttpHeaders.SET_COOKIE, sessionCookie.toString());

                                    if (refreshToken != null) {
                                        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                                            .httpOnly(true).secure(true).path("/").maxAge(Duration.ofDays(7)).sameSite("Lax").build();
                                        builder = builder.header(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                                    }

                                    log.info("[Auth] 로그인 성공 → session_id 발급: {}", sessionId);
                                    saveLoginLog(username, resolvedIp, "SUCCESS");
                                    resetBruteForceCounter(resolvedIp);
                                    return builder.body(entity.getBody());
                                })));
                    }
                }
                return Mono.just(ResponseEntity.<Map<String, Object>>status(entity.getStatusCode()).body(entity.getBody()));
            })
            .onErrorResume(WebClientResponseException.class, e -> {
                log.warn("[Auth] 로그인 실패: status={}", e.getStatusCode());
                saveLoginLog(username, resolvedIp, "FAIL");
                tryBruteForceBlock(resolvedIp);
                return Mono.just(ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다.")));
            })
            .onErrorResume((Throwable e) -> {
                log.error("[Auth] 인증서버 연결 실패: {}", e.getMessage());
                saveLoginLog(username, resolvedIp, "ERROR");
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "인증 서버에 연결할 수 없습니다.")));
            });
    }

    private void saveLoginLog(String username, String clientIp, String result) {
        LoginLogEntity entry = new LoginLogEntity();
        entry.setUsername(username);
        entry.setClientIp(clientIp);
        entry.setResult(result);
        entry.setCreatedAt(LocalDateTime.now());
        loginLogRepository.save(entry).subscribe();
    }

    private void tryBruteForceBlock(String ip) {
        if (!bruteForceEnabled || "unknown".equals(ip)) return;
        String key = BRUTE_FORCE_KEY_PREFIX + ip;
        redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    return redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
                }
                if (count >= maxAttempts) {
                    return ipBlacklistRepository.findByIpAddress(ip)
                        .switchIfEmpty(Mono.defer(() -> {
                            IpBlacklistEntity entity = new IpBlacklistEntity();
                            entity.setIpAddress(ip);
                            entity.setReason("브루트포스 차단 (로그인 실패 " + maxAttempts + "회 연속)");
                            entity.setCreatedAt(LocalDateTime.now());
                            entity.setExpiredAt(LocalDateTime.now().plusHours(blockHours));
                            entity.setAuto(true);
                            return ipBlacklistRepository.save(entity)
                                .doOnNext(saved -> {
                                    ipBlacklistFilter.refreshCache();
                                    redisTemplate.delete(key).subscribe();
                                    log.warn("[BruteForce] IP 자동 차단: {}", ip);
                                });
                        }))
                        .then();
                }
                return Mono.empty();
            })
            .subscribe();
    }

    private void resetBruteForceCounter(String ip) {
        if (!bruteForceEnabled || "unknown".equals(ip)) return;
        redisTemplate.delete(BRUTE_FORCE_KEY_PREFIX + ip).subscribe();
    }

    /**
     * session_id 쿠키로 세션 유효성 검증.
     * Redis에서 access_token을 꺼내 인증서버에 검증 요청하고,
     * 만료 시 refresh_token으로 자동 갱신 후 재검증한다.
     * 브라우저(ERP 프론트)와 ERP 서버 모두 이 엔드포인트를 호출한다.
     */
    @GetMapping("/session/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateSession(
            @CookieValue(value = "session_id", required = false) String sessionId) {

        if (sessionId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "세션이 없습니다.")));
        }

        log.info("[Auth] session/validate 호출: sessionId={}", sessionId);
        return loadSession(sessionId)
            .flatMap(session -> doValidate(sessionId, session))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "세션이 만료되었거나 존재하지 않습니다."))));
    }

    private Mono<ResponseEntity<Map<String, Object>>> doValidate(
            String sessionId, Map<String, String> session) {

        String accessToken = session.get("accessToken");
        if (accessToken == null || accessToken.isEmpty()) {
            return Mono.just(ResponseEntity.<Map<String, Object>>status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "세션에 토큰이 없습니다.")));
        }

        return webClient.get()
            .uri(authServerUrl + "/api/auth/validate")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(res -> ResponseEntity.<Map<String, Object>>status(res.getStatusCode()).body(res.getBody()))
            .onErrorResume(WebClientResponseException.class, e -> {
                if (e.getStatusCode().value() == 401) {
                    String refreshToken = session.get("refreshToken");
                    if (refreshToken != null && !refreshToken.isEmpty()) {
                        return doRefresh(sessionId, session, refreshToken);
                    }
                    return deleteSession(sessionId)
                        .then(Mono.just(ResponseEntity.<Map<String, Object>>status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "세션이 만료되었습니다."))));
                }
                log.warn("[Auth] validate 실패: status={}", e.getStatusCode());
                return Mono.just(ResponseEntity.<Map<String, Object>>status(e.getStatusCode())
                    .body(Map.of("message", "토큰 검증 실패")));
            });
    }

    private Mono<ResponseEntity<Map<String, Object>>> doRefresh(
            String sessionId, Map<String, String> session, String refreshToken) {

        log.info("[Auth] access_token 만료 → refresh 시도: sessionId={}", sessionId);
        return webClient.post()
            .uri(authServerUrl + "/api/auth/refresh")
            .bodyValue(Map.of("refreshToken", refreshToken))
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .<ResponseEntity<Map<String, Object>>>flatMap(res -> {
                String newAccess  = res.getBody() != null ? (String) res.getBody().get("access_token")  : null;
                String newRefresh = res.getBody() != null ? (String) res.getBody().get("refresh_token") : null;
                if (newAccess == null) {
                    return deleteSession(sessionId)
                        .then(Mono.<ResponseEntity<Map<String, Object>>>just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "세션이 만료되었습니다."))));
                }
                Map<String, String> updated = new HashMap<>(session);
                updated.put("accessToken", newAccess);
                if (newRefresh != null) updated.put("refreshToken", newRefresh);
                return saveSession(sessionId, updated)
                    .then(doValidate(sessionId, updated));
            })
            .onErrorResume((Throwable e) -> {
                log.warn("[Auth] refresh 실패: {}", e.getMessage());
                return deleteSession(sessionId)
                    .then(Mono.just(ResponseEntity.<Map<String, Object>>status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "세션이 만료되었습니다."))));
            });
    }

    /** 기존 JWT Bearer / 쿠키 방식 validate (게이트웨이 내부 필터용) */
    @GetMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(value = "jwt_token", required = false) String cookieToken) {
        String effectiveAuth = authHeader != null ? authHeader
            : (cookieToken != null ? "Bearer " + cookieToken : null);

        if (effectiveAuth == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "인증 토큰이 없습니다.")));
        }

        return webClient.get()
            .uri(authServerUrl + "/api/auth/validate")
            .header(HttpHeaders.AUTHORIZATION, effectiveAuth)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .onErrorResume(WebClientResponseException.class, e -> {
                log.warn("[Auth] 토큰 검증 실패: status={}", e.getStatusCode());
                return Mono.just(ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("message", "유효하지 않은 토큰입니다.")));
            })
            .onErrorResume((Throwable e) -> {
                log.error("[Auth] 인증서버 연결 실패: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "인증 서버에 연결할 수 없습니다.")));
            });
    }

    /** 토큰 갱신 — { refreshToken } → 인증서버 /api/auth/refresh 프록시 */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Map<String, Object>>> refresh(@RequestBody Map<String, String> body) {
        log.debug("[Auth] 토큰 갱신 요청");
        return webClient.post()
            .uri(authServerUrl + "/api/auth/refresh")
            .bodyValue(body)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
            .onErrorResume(WebClientResponseException.class, e -> {
                log.warn("[Auth] 토큰 갱신 실패: status={}", e.getStatusCode());
                return Mono.just(ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("message", "토큰 갱신에 실패했습니다.")));
            })
            .onErrorResume((Throwable e) -> {
                log.error("[Auth] 인증서버 연결 실패: {}", e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "인증 서버에 연결할 수 없습니다.")));
            });
    }

    /** 로그아웃 — 인증서버 프록시 + Redis 세션 삭제 + 쿠키 전체 삭제 */
    @PostMapping("/logout")
    public Mono<ResponseEntity<String>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @CookieValue(value = "jwt_token",    required = false) String cookieToken,
            @CookieValue(value = "session_id",   required = false) String sessionId,
            @RequestBody(required = false) Map<String, String> body) {
        log.debug("[Auth] 로그아웃 요청");

        String effectiveAuth = authHeader != null ? authHeader
            : (cookieToken != null ? "Bearer " + cookieToken : null);

        ResponseCookie clearJwt = ResponseCookie.from("jwt_token",    "").httpOnly(true).secure(true).path("/").maxAge(Duration.ZERO).sameSite("Lax").build();
        ResponseCookie clearSid = ResponseCookie.from("session_id",   "").httpOnly(true).secure(true).path("/").maxAge(Duration.ZERO).sameSite("Lax").build();
        ResponseCookie clearRef = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(true).path("/").maxAge(Duration.ZERO).sameSite("Lax").build();

        Mono<Void> redisDelete = sessionId != null ? deleteSession(sessionId).then() : Mono.empty();

        WebClient.RequestBodySpec requestSpec = webClient.post().uri(authServerUrl + "/api/auth/logout");
        if (effectiveAuth != null) requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, effectiveAuth);
        WebClient.RequestHeadersSpec<?> headersSpec = (body != null) ? requestSpec.bodyValue(body) : requestSpec;

        return redisDelete.then(
            headersSpec.retrieve()
                .toEntity(String.class)
                .map(entity -> ResponseEntity.status(entity.getStatusCode())
                    .header(HttpHeaders.SET_COOKIE, clearJwt.toString())
                    .header(HttpHeaders.SET_COOKIE, clearSid.toString())
                    .header(HttpHeaders.SET_COOKIE, clearRef.toString())
                    .body(entity.getBody()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.warn("[Auth] 로그아웃 실패: status={}", e.getStatusCode());
                    return Mono.just(ResponseEntity.status(e.getStatusCode())
                        .header(HttpHeaders.SET_COOKIE, clearJwt.toString())
                        .header(HttpHeaders.SET_COOKIE, clearSid.toString())
                        .header(HttpHeaders.SET_COOKIE, clearRef.toString())
                        .body(e.getResponseBodyAsString()));
                })
                .onErrorResume((Throwable e) -> {
                    log.warn("[Auth] 로그아웃 처리 중 오류: {}", e.getMessage());
                    return Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, clearJwt.toString())
                        .header(HttpHeaders.SET_COOKIE, clearSid.toString())
                        .header(HttpHeaders.SET_COOKIE, clearRef.toString())
                        .<String>build());
                })
        );
    }

    // ── Session ID 생성 ─────────────────────────────────────────────────────────

    private Mono<String> generateUniqueSessionId() {
        String sessionId = UUID.randomUUID().toString();
        return redisTemplate.hasKey(SESSION_KEY_PREFIX + sessionId)
            .flatMap(exists -> exists ? generateUniqueSessionId() : Mono.just(sessionId));
    }

    // ── Redis helpers ──────────────────────────────────────────────────────────

    private Mono<Void> saveSession(String sessionId, Map<String, String> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return redisTemplate.opsForValue()
                .set(SESSION_KEY_PREFIX + sessionId, json, SESSION_TTL)
                .then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private Mono<Map<String, String>> loadSession(String sessionId) {
        return redisTemplate.opsForValue()
            .get(SESSION_KEY_PREFIX + sessionId)
            .mapNotNull(json -> {
                try {
                    return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                } catch (Exception e) {
                    log.warn("[Auth] 세션 JSON 파싱 실패: {}", e.getMessage());
                    return null;
                }
            });
    }

    private Mono<Long> deleteSession(String sessionId) {
        return redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }
}
