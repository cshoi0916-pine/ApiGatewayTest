package com.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String ip = getClientIp(exchange);

        log.info("[REQUEST]  method={}, path={}, ip={}, traceId={}", method, path, ip, traceId);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Gateway", "api-gateway")
                .header("X-Trace-Id", traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("[RESPONSE] status={}, duration={}ms, traceId={}", status, duration, traceId);
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
