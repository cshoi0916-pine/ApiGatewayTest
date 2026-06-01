package com.example.apigateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApiMetricsService {

    private final MeterRegistry registry;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalBlocked  = new AtomicLong(0);

    private final ConcurrentHashMap<String, AtomicLong> blockedByIp = new ConcurrentHashMap<>();

    private final Counter requestCounter;
    private final Counter blockedCounter;

    public ApiMetricsService(MeterRegistry registry) {
        this.registry = registry;
        this.requestCounter = Counter.builder("gateway_requests_total")
                .description("총 요청 수")
                .register(registry);
        this.blockedCounter = Counter.builder("gateway_blocked_total")
                .description("Rate Limit 차단 수")
                .register(registry);
    }

    public void recordRequest(String ip) {
        requestCounter.increment();
        totalRequests.incrementAndGet();
    }

    public void recordBlocked(String ip) {
        blockedCounter.increment();
        totalBlocked.incrementAndGet();
        blockedByIp.computeIfAbsent(ip, k -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalRequests.get());
        stats.put("totalBlocked", totalBlocked.get());
        stats.put("blockedByIp", new HashMap<>(blockedByIp));
        return stats;
    }
}
