package com.example.apigateway.filter;

import com.example.apigateway.exception.GatewayServiceException;
import com.example.apigateway.registry.ServiceRegistryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleCircuitBreakerFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SimpleCircuitBreakerFilter.class);

    private final ConcurrentHashMap<String, AtomicInteger> failures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> openedAt = new ConcurrentHashMap<>();
    // 관리자가 강제 OPEN한 서비스 목록 — 시간 기반 자동 해제 없이 forceClose()까지 유지
    private final Set<String> forcedOpen = ConcurrentHashMap.newKeySet();
    // HALF_OPEN 테스트 요청 진행 중인 서비스 — 딱 1개만 통과시키기 위한 플래그
    private final Set<String> halfOpenTesting = ConcurrentHashMap.newKeySet();

    private final ServiceRegistryService registryService;
    private final CircuitEventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${circuit-breaker.open-duration-ms:30000}")
    private long openDurationMs;

    public SimpleCircuitBreakerFilter(ServiceRegistryService registryService,
                                       CircuitEventRepository eventRepository) {
        this.registryService = registryService;
        this.eventRepository = eventRepository;
    }

    private void saveEvent(String serviceId, String event, String detail) {
        CircuitEventEntity e = new CircuitEventEntity();
        e.setServiceId(serviceId);
        e.setEvent(event);
        e.setDetail(detail);
        e.setEventAt(LocalDateTime.now());
        eventRepository.save(e).subscribe();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) return chain.filter(exchange);
        URI uri = route.getUri();
        if (!"lb".equals(uri.getScheme())) return chain.filter(exchange);

        String serviceId = uri.getHost();

        Long openTime = openedAt.get(serviceId);
        if (openTime != null) {
            boolean isForced = forcedOpen.contains(serviceId);
            long elapsed = System.currentTimeMillis() - openTime;

            if (isForced || elapsed < openDurationMs) {
                long retryAfter = isForced ? -1 : (openDurationMs - elapsed) / 1000;
                log.debug("[CircuitBreaker] OPEN 거부: serviceId={} forced={}", serviceId, isForced);
                return writeCircuitOpenResponse(exchange, serviceId, retryAfter);
            }

            // HALF_OPEN: Set.add()는 원자적 — 처음 들어온 요청만 true, 나머지는 false(거부)
            if (!halfOpenTesting.add(serviceId)) {
                log.debug("[CircuitBreaker] HALF-OPEN 테스트 진행 중, 거부: {}", serviceId);
                return writeCircuitOpenResponse(exchange, serviceId, 0);
            }
            log.info("[CircuitBreaker] HALF-OPEN 테스트 시작: {}", serviceId);
        }

        boolean isHalfOpen = halfOpenTesting.contains(serviceId);

        return chain.filter(exchange)
            .then(Mono.<Void>fromRunnable(() -> {
                int statusCode = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 0;
                if (statusCode >= 500) {
                    halfOpenTesting.remove(serviceId);
                    if (isHalfOpen) {
                        openedAt.put(serviceId, System.currentTimeMillis());
                        log.warn("[CircuitBreaker] HALF-OPEN 테스트 실패, 재OPEN: {}", serviceId);
                    }
                    recordFailure(serviceId);
                } else {
                    halfOpenTesting.remove(serviceId);
                    openedAt.remove(serviceId);
                    failures.remove(serviceId);
                    if (isHalfOpen) {
                        log.info("[CircuitBreaker] HALF-OPEN 테스트 성공, CLOSED: {}", serviceId);
                        saveEvent(serviceId, "CLOSED", "HALF_OPEN 테스트 성공");
                    }
                }
            }))
            .doOnError(e -> {
                if (isNetworkError(e)) {
                    halfOpenTesting.remove(serviceId);
                    if (isHalfOpen) {
                        openedAt.put(serviceId, System.currentTimeMillis());
                        log.warn("[CircuitBreaker] HALF-OPEN 테스트 실패(연결 에러), 재OPEN: {}", serviceId);
                    }
                    registryService.evictCache(serviceId).subscribe();
                    recordFailure(serviceId);
                }
            });
    }

    private void recordFailure(String serviceId) {
        int count = failures.computeIfAbsent(serviceId, k -> new AtomicInteger()).incrementAndGet();
        if (count >= failureThreshold) {
            openedAt.put(serviceId, System.currentTimeMillis());
            log.warn("[CircuitBreaker] OPEN: serviceId={} ({}회 연속 실패)", serviceId, count);
            saveEvent(serviceId, "OPEN", "연속 실패 " + count + "회");
        }
    }

    private boolean isNetworkError(Throwable e) {
        if (e instanceof GatewayServiceException) return true;
        String cn = e.getClass().getName();
        return cn.contains("ConnectException") || cn.contains("TimeoutException") || cn.contains("ClosedChannelException");
    }

    private Mono<Void> writeCircuitOpenResponse(ServerWebExchange exchange, String serviceId, long retryAfterSec) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfterSec));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("message", "서비스 점검 중입니다. " + retryAfterSec + "초 후 다시 시도해주세요. [" + serviceId + "]");
        body.put("serviceId", serviceId);
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("timestamp", LocalDateTime.now().toString());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    public void forceOpen(String serviceId) {
        forcedOpen.add(serviceId);
        failures.computeIfAbsent(serviceId, k -> new AtomicInteger()).set(failureThreshold);
        openedAt.put(serviceId, System.currentTimeMillis());
        halfOpenTesting.remove(serviceId);
        log.info("[CircuitBreaker] 강제 OPEN: serviceId={}", serviceId);
        saveEvent(serviceId, "FORCED_OPEN", "관리자 강제 차단");
    }

    public void forceClose(String serviceId) {
        forcedOpen.remove(serviceId);
        openedAt.remove(serviceId);
        failures.remove(serviceId);
        halfOpenTesting.remove(serviceId);
        log.info("[CircuitBreaker] 강제 CLOSE: serviceId={}", serviceId);
        saveEvent(serviceId, "FORCED_CLOSE", "관리자 강제 복구");
    }

    public Map<String, Object> getServiceStatus(String serviceId) {
        Long openTime = openedAt.get(serviceId);
        int failCount = failures.containsKey(serviceId) ? failures.get(serviceId).get() : 0;
        boolean isForced = forcedOpen.contains(serviceId);

        String state;
        long retryAfterSec = 0;
        if (openTime != null) {
            long elapsed = System.currentTimeMillis() - openTime;
            if (isForced || elapsed < openDurationMs) {
                state = "OPEN";
                retryAfterSec = isForced ? -1 : (openDurationMs - elapsed) / 1000;
            } else {
                state = "HALF_OPEN";
            }
        } else {
            state = "CLOSED";
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("state", state);
        m.put("failureCount", failCount);
        m.put("failureThreshold", failureThreshold);
        m.put("retryAfterSec", retryAfterSec);
        m.put("forced", isForced);
        return m;
    }

    @Override
    public int getOrder() {
        return 1;  // ApiKeyFilter(-50), IpRateLimitFilter(-1) 이후, LoadBalancer 이전
    }
}
