package com.example.apigateway.filter;

import com.example.apigateway.metrics.ApiMetricsService;
import com.example.apigateway.metrics.RequestLogEntity;
import com.example.apigateway.metrics.RequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private final ApiMetricsService metricsService;
    private final RequestLogRepository logRepository;

    public LoggingFilter(ApiMetricsService metricsService, RequestLogRepository logRepository) {
        this.metricsService = metricsService;
        this.logRepository = logRepository;
    }

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

                    // 실제 라우팅된 백엔드 인스턴스 기록
                    URI routedUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    String serviceId = null;
                    if (route != null && route.getUri() != null) {
                        serviceId = route.getUri().getHost(); // lb://test-backend → test-backend
                    }
                    String instanceAddress = null;
                    if (routedUri != null) {
                        instanceAddress = routedUri.getHost() + ":" + routedUri.getPort();
                        metricsService.recordInstance(instanceAddress);
                        log.info("[RESPONSE] status={}, duration={}ms, instance={}, traceId={}",
                                status, duration, instanceAddress, traceId);
                    } else {
                        log.info("[RESPONSE] status={}, duration={}ms, traceId={}",
                                status, duration, traceId);
                    }

                    // DB 이력 저장 (fire-and-forget)
                    RequestLogEntity entry = new RequestLogEntity();
                    entry.setMethod(method);
                    entry.setPath(path);
                    entry.setServiceId(serviceId);
                    entry.setStatusCode(status);
                    entry.setDurationMs(duration);
                    entry.setClientIp(ip);
                    entry.setInstanceAddress(instanceAddress);
                    entry.setCreatedAt(LocalDateTime.now());
                    logRepository.save(entry).subscribe();
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
