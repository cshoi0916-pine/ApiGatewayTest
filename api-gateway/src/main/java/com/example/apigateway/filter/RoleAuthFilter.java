package com.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RoleAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RoleAuthFilter.class);

    // JwtAuthFilter와 동일한 화이트리스트 — JWT 미검증 경로는 역할 확인도 불필요
    private static final List<String> WHITELIST = List.of(
        "/auth/", "/public/", "/console", "/actuator", "/registry"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final RoleAuthProperties props;

    public RoleAuthFilter(RoleAuthProperties props) {
        this.props = props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.isEnabled() || props.getRules().isEmpty()) return chain.filter(exchange);

        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) return chain.filter(exchange);

        // 경로와 매칭되는 첫 번째 룰 적용 (없으면 통과 — 기본값: 제한 없음)
        RoleAuthProperties.RoleRule matched = props.getRules().stream()
            .filter(r -> pathMatcher.match(r.getPath(), path))
            .findFirst()
            .orElse(null);

        if (matched == null) return chain.filter(exchange);

        String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
        if (role != null && matched.getRoles().contains(role)) {
            return chain.filter(exchange);
        }

        log.warn("[RoleAuth] 권한 없음: path={} role={} required={}", path, role, matched.getRoles());
        return writeForbidden(exchange, path);
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeForbidden(ServerWebExchange exchange, String path) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status":403,"error":"Forbidden","message":"접근 권한이 없습니다.","path":"%s"}
                """.formatted(path);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -30;
    }
}
