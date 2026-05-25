package com.example.apigateway.filter;

import com.example.apigateway.apikey.ApiClient;
import com.example.apigateway.apikey.ApiClientRegistry;
import com.example.apigateway.apikey.ApiClientRepository;
import com.example.apigateway.metrics.ApiMetricsService;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyFilter implements GlobalFilter, Ordered {

//    private final ApiClientRegistry registry;
    private final ApiClientRepository repository;
    private final ApiMetricsService metricsService;

    public ApiKeyFilter(
            ApiClientRepository repository,
            ApiMetricsService metricsService
    ) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        if (path.startsWith("/auth")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest()
                .getHeaders()
                .getFirst("X-API-KEY");

        if (apiKey == null) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        ApiClient client = repository
                .findByApiKey(apiKey)
                .orElse(null);

        if (client == null) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        metricsService.increaseRequest(client.getName());

        exchange.getAttributes().put("apiClient", client);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -3;
    }
}