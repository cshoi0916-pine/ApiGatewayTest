package com.example.apigateway.filter;

import com.example.apigateway.apikey.ApiClient;
import com.example.apigateway.apikey.ApiClientRepository;
import com.example.apigateway.metrics.ApiMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final ApiClientRepository repository;
    private final ApiMetricsService metricsService;

    public ApiKeyFilter(ApiClientRepository repository, ApiMetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // API Key 인증 제외 경로
        if (path.startsWith("/auth") || path.startsWith("/gateway/admin")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

        if (apiKey == null) {
            log.warn("[API-KEY] missing X-API-KEY header, path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        ApiClient client = repository.findByApiKey(apiKey).orElse(null);

        if (client == null) {
            log.warn("[API-KEY] unknown key={}, path={}", apiKey, path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        log.info("[API-KEY] client={}, path={}", client.getName(), path);
        metricsService.increaseRequest(client.getName());
        exchange.getAttributes().put("apiClient", client);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
