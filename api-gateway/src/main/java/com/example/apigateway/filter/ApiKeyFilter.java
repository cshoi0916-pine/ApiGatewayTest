package com.example.apigateway.filter;

import com.example.apigateway.apikey.ApiKeyService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ApiKeyFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final ApiKeyService apiKeyService;
    private final DatabaseClient databaseClient;

    private volatile boolean enabled = false;

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("[API-KEY] 기능 상태 변경: {}", enabled ? "ON" : "OFF");
        databaseClient.sql("""
                    INSERT INTO gateway.gateway_setting (key, value)
                    VALUES ('apikey-enabled', :value)
                    ON CONFLICT (key) DO UPDATE SET value = :value, updated_at = NOW()
                    """)
                .bind("value", String.valueOf(enabled))
                .fetch().rowsUpdated()
                .subscribe(n -> log.debug("[API-KEY] DB 저장 완료"),
                           e -> log.warn("[API-KEY] DB 저장 실패: {}", e.getMessage()));
    }

    public ApiKeyFilter(ApiKeyService apiKeyService, DatabaseClient databaseClient) {
        this.apiKeyService = apiKeyService;
        this.databaseClient = databaseClient;
    }

    @PostConstruct
    public void loadFromDb() {
        try {
            String dbValue = databaseClient
                .sql("SELECT value FROM gateway.gateway_setting WHERE key = 'apikey-enabled'")
                .map(row -> row.get("value", String.class))
                .first()
                .block(Duration.ofSeconds(5));
            if (dbValue != null) {
                this.enabled = Boolean.parseBoolean(dbValue);
                log.info("[API-KEY] DB에서 로드: {}", enabled ? "ON" : "OFF");
            } else {
                log.info("[API-KEY] DB 설정 없음, 기본값 OFF 사용");
            }
        } catch (Exception e) {
            log.warn("[API-KEY] DB 로드 실패, 기본값 OFF 사용: {}", e.getMessage());
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/console") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        boolean routeRequiresApiKey = route != null &&
                Boolean.TRUE.equals(route.getMetadata().get("api-key-required"));
        if (!routeRequiresApiKey) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[API-KEY] 키 없음 path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return apiKeyService.isValidKey(apiKey)
                .flatMap(valid -> {
                    if (!valid) {
                        log.warn("[API-KEY] 유효하지 않은 키 path={}", path);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getAttributes().put("AUTHENTICATED_BY_API_KEY", true);
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
