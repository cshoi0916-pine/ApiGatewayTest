package com.example.apigateway.filter;

import com.example.apigateway.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain
    ) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        log.info("[JWT] filter start path={}", path);

        // 인증 제외 경로
        if (path.startsWith("/auth")) {

            log.info("[JWT] auth path skip");

            return chain.filter(exchange);
        }

        String authHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);
        System.out.println("HEADER = [" + authHeader + "]");
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            log.warn("[JWT] invalid authorization header");

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        String token =
                authHeader.substring(7);

        boolean valid =
                JwtUtil.validate(token);

        if (!valid) {

            log.warn("[JWT] token invalid");

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        String userId =
                JwtUtil.getUserId(token);

        log.info("[JWT] authenticated user={}", userId);

        // downstream header 추가
        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header("X-USER-ID", userId)
                        .build();

        ServerWebExchange mutatedExchange =
                exchange.mutate()
                        .request(mutatedRequest)
                        .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {

        // API KEY보다 먼저
        return -4;
    }
}