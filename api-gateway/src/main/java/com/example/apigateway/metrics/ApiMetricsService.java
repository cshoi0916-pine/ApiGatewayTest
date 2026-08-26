package com.example.apigateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class  ApiMetricsService {

    private final MeterRegistry registry;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalBlocked  = new AtomicLong(0);

    // 인스턴스별 요청 수 (예: "192.168.1.99:8081" -> 42)
    private final ConcurrentHashMap<String, AtomicLong> requestsByInstance = new ConcurrentHashMap<>();
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

    // 실제 라우팅된 백엔드 인스턴스 기록
    public void recordInstance(String instanceKey) {
        requestsByInstance
                .computeIfAbsent(instanceKey, k -> new AtomicLong(0))
                .incrementAndGet();

        Counter.builder("gateway_instance_requests_total")
                .tag("instance", instanceKey)
                .register(registry)
                .increment();
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

        // 인스턴스별 요청 수 Map으로 변환
        Map<String, Long> instanceStats = new HashMap<>();
        requestsByInstance.forEach((k, v) -> instanceStats.put(k, v.get()));
        stats.put("requestsByInstance", instanceStats);

        Map<String, Long> blockedStats = new HashMap<>();
        blockedByIp.forEach((k, v) -> blockedStats.put(k, v.get()));
        stats.put("blockedByIp", blockedStats);

        return stats;
    }
}
