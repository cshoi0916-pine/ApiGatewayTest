package com.example.apigateway.filter;

import com.example.apigateway.metrics.ApiMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class IpRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IpRateLimitFilter.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiMetricsService metricsService;

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    public IpRateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
                             ApiMetricsService metricsService) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String ip = getClientIp(exchange);
        String key = "rate:" + ip;

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    if (count == 1) {
                        redisTemplate.expire(key, Duration.ofSeconds(60)).subscribe();
                    }

                    if (count > requestsPerMinute) {
                        log.warn("[RATE-LIMIT] blocked ip={}, count={}/{}", ip, count, requestsPerMinute);
                        metricsService.recordBlocked(ip);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    metricsService.recordRequest(ip);
                    return chain.filter(exchange);
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        // 프록시 환경 대응
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
