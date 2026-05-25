package com.example.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AdminFilter implements GlobalFilter, Ordered {

    @Value("${admin.key}")
    private String adminKey;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain
    ) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        if (!path.startsWith("/admin")) {
            return chain.filter(exchange);
        }

        String requestKey = exchange.getRequest()
                .getHeaders()
                .getFirst("X-ADMIN-KEY");

        if (requestKey == null ||
                !requestKey.equals(adminKey)) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.FORBIDDEN);

            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -10;
    }
}