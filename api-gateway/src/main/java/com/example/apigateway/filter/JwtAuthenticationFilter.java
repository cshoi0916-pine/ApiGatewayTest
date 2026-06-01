package com.example.apigateway.filter;

import com.example.apigateway.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 인증 제외 경로
        if (path.startsWith("/auth") || path.startsWith("/gateway/admin")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT] missing or invalid Authorization header, path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validate(token)) {
            log.warn("[JWT] token validation failed, path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);

        log.info("[JWT] authenticated user={}, role={}, path={}", userId, role, path);

        // /api/admin/** 은 ADMIN 권한 필요
        if (path.startsWith("/api/admin")) {
            if (!"ADMIN".equals(role)) {
                log.warn("[JWT] forbidden: user={}, role={}, path={}", userId, role, path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        // 백엔드에 사용자 정보 전달
        ServerWebExchange mutated = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .build())
                .build();

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
