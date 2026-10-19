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

import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleCircuitBreakerFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SimpleCircuitBreakerFilter.class);

    /** 슬라이딩 윈도우: 서비스별 최근 N건 요청 결과 (true=성공, false=실패) */
    private final ConcurrentHashMap<String, ArrayDeque<Boolean>> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> openedAt = new ConcurrentHashMap<>();
    private final Set<String> forcedOpen = ConcurrentHashMap.newKeySet();
    private final Set<String> halfOpenTesting = ConcurrentHashMap.newKeySet();

    private final ServiceRegistryService registryService;
    private final CircuitEventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${circuit-breaker.failure-threshold:5}")
    private int failureThreshold;

    @Value("${circuit-breaker.open-duration-ms:30000}")
    private long openDurationMs;

    @Value("${circuit-breaker.window-size:10}")
    private int windowSize;

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
                log.debug("[CB] .then() serviceId={} statusCode={} isHalfOpen={}", serviceId, statusCode, isHalfOpen);
                if (statusCode >= 500) {
                    halfOpenTesting.remove(serviceId);
                    if (isHalfOpen) {
                        openedAt.put(serviceId, System.currentTimeMillis());
                        log.warn("[CircuitBreaker] HALF-OPEN 테스트 실패, 재OPEN: {}", serviceId);
                    }
                    checkAndTrip(serviceId, false);
                } else {
                    halfOpenTesting.remove(serviceId);
                    if (isHalfOpen) {
                        openedAt.remove(serviceId);
                        resetWindow(serviceId);
                        log.info("[CircuitBreaker] HALF-OPEN 테스트 성공, CLOSED: {}", serviceId);
                        saveEvent(serviceId, "CLOSED", "HALF_OPEN 테스트 성공");
                    } else {
                        recordResult(serviceId, true);
                    }
                }
            }))
            .doOnError(e -> {
                log.debug("[CB] .doOnError() serviceId={} error={}", serviceId, e.getClass().getName());
                if (isNetworkError(e)) {
                    halfOpenTesting.remove(serviceId);
                    if (isHalfOpen) {
                        openedAt.put(serviceId, System.currentTimeMillis());
                        log.warn("[CircuitBreaker] HALF-OPEN 테스트 실패(연결 에러), 재OPEN: {}", serviceId);
                    }
                    registryService.evictCache(serviceId).subscribe();
                    checkAndTrip(serviceId, false);
                }
            });
    }

    /**
     * 슬라이딩 윈도우에 결과를 추가하고, 실패율이 임계치를 넘으면 OPEN으로 전환한다.
     */
    private void checkAndTrip(String serviceId, boolean success) {
        int failCount = recordResult(serviceId, success);
        int total = getWindowCount(serviceId);
        if (!success && total >= failureThreshold && failCount >= failureThreshold) {
            openedAt.put(serviceId, System.currentTimeMillis());
            log.warn("[CircuitBreaker] OPEN: serviceId={} (최근 {}건 중 {}건 실패)", serviceId, total, failCount);
            saveEvent(serviceId, "OPEN", "최근 " + total + "건 중 " + failCount + "건 실패");
        }
    }

    /** 윈도우에 결과 추가 → 실패 횟수 반환 */
    private int recordResult(String serviceId, boolean success) {
        ArrayDeque<Boolean> window = windows.computeIfAbsent(serviceId, k -> new ArrayDeque<>());
        synchronized (window) {
            window.addLast(success);
            if (window.size() > windowSize) {
                window.pollFirst();
            }
            return (int) window.stream().filter(r -> !r).count();
        }
    }

    private int getWindowCount(String serviceId) {
        ArrayDeque<Boolean> window = windows.get(serviceId);
        if (window == null) return 0;
        synchronized (window) { return window.size(); }
    }

    private void resetWindow(String serviceId) {
        ArrayDeque<Boolean> window = windows.get(serviceId);
        if (window != null) {
            synchronized (window) { window.clear(); }
        }
    }

    private boolean isNetworkError(Throwable e) {
        if (e instanceof GatewayServiceException) return true;
        // SCG가 타임아웃/라우팅 오류를 ResponseStatusException으로 래핑하는 경우 (504 등)
        if (e instanceof ResponseStatusException rse) {
            return rse.getStatusCode().is5xxServerError();
        }
        String cn = e.getClass().getName();
        return cn.contains("ConnectException")
            || cn.contains("TimeoutException")
            || cn.contains("ClosedChannelException")
            || cn.contains("NotFoundException")
            || cn.contains("PrematureCloseException")
            || cn.contains("ReadTimeoutException");
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
        openedAt.put(serviceId, System.currentTimeMillis());
        halfOpenTesting.remove(serviceId);
        log.info("[CircuitBreaker] 강제 OPEN: serviceId={}", serviceId);
        saveEvent(serviceId, "FORCED_OPEN", "관리자 강제 차단");
    }

    public void forceClose(String serviceId) {
        forcedOpen.remove(serviceId);
        openedAt.remove(serviceId);
        resetWindow(serviceId);
        halfOpenTesting.remove(serviceId);
        log.info("[CircuitBreaker] 강제 CLOSE: serviceId={}", serviceId);
        saveEvent(serviceId, "FORCED_CLOSE", "관리자 강제 복구");
    }

    public Map<String, Object> getServiceStatus(String serviceId) {
        Long openTime = openedAt.get(serviceId);
        boolean isForced = forcedOpen.contains(serviceId);

        int failCount = 0;
        int windowCount = 0;
        ArrayDeque<Boolean> window = windows.get(serviceId);
        if (window != null) {
            synchronized (window) {
                windowCount = window.size();
                failCount = (int) window.stream().filter(r -> !r).count();
            }
        }

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
        m.put("windowCount", windowCount);
        m.put("failureThreshold", failureThreshold);
        m.put("windowSize", windowSize);
        m.put("retryAfterSec", retryAfterSec);
        m.put("forced", isForced);
        return m;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
