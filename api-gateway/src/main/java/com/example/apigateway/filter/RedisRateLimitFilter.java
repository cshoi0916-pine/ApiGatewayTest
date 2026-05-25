package com.example.apigateway.filter;

import com.example.apigateway.apikey.ApiClient;
import com.example.apigateway.metrics.ApiMetricsService;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisRateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiMetricsService metricsService;

    public RedisRateLimitFilter(
            ReactiveStringRedisTemplate redisTemplate,
            ApiMetricsService metricsService
    ) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain
    ) {

        ApiClient client =
                exchange.getAttribute("apiClient");

        if (client == null) {
            return chain.filter(exchange);
        }

        String key =
                "rate:" + client.getApiKey();

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    if (count == 1) {
                        redisTemplate.expire(
                                key,
                                Duration.ofSeconds(60)
                        ).subscribe();
                    }

                    if (count > client.getLimitPerMinute()) {

                        metricsService.increaseBlocked(
                                client.getName()
                        );

                        exchange.getResponse()
                                .setStatusCode(
                                        HttpStatus.TOO_MANY_REQUESTS
                                );

                        return exchange.getResponse()
                                .setComplete();
                    }

                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -2;
    }
}