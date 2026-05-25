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

    private static final Logger log =
            LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        String client = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

        String traceId = UUID.randomUUID().toString();

        log.info("요청 시작: method={}, path={}, client={}, traceId={}",
                method, path, client, traceId);

        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header("X-Gateway", "api-gateway")
                        .header("X-TRACE-ID", traceId)
                        .build();

        ServerWebExchange mutatedExchange =
                exchange.mutate()
                        .request(mutatedRequest)
                        .build();

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {

                    long duration = System.currentTimeMillis() - startTime;

                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("요청 종료: status={}, time={}ms, traceId={}",
                            statusCode,
                            duration,
                            traceId);
                });
    }

    @Override
    public int getOrder() {
        return 1;
    }
}