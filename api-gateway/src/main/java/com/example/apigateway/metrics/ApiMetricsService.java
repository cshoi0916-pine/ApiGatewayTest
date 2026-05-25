package com.example.apigateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApiMetricsService {

    private final MeterRegistry registry;

    private final ConcurrentHashMap<String, Counter> requestCounters =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Counter> blockedCounters =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Double> requestCounts =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Double> blockedCounts =
            new ConcurrentHashMap<>();

    public ApiMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void increaseRequest(String clientName) {

        Counter counter = requestCounters.computeIfAbsent(
                clientName,
                name -> Counter.builder("api_requests_total")
                        .tag("client", name)
                        .register(registry)
        );

        counter.increment();

        requestCounts.merge(clientName, 1.0, Double::sum);
    }

    public void increaseBlocked(String clientName) {

        Counter counter = blockedCounters.computeIfAbsent(
                clientName,
                name -> Counter.builder("api_blocked_total")
                        .tag("client", name)
                        .register(registry)
        );

        counter.increment();

        blockedCounts.merge(clientName, 1.0, Double::sum);
    }

    public Map<String, Object> getStats() {

        Map<String, Object> result = new HashMap<>();

        for (String client : requestCounts.keySet()) {

            Map<String, Object> data = new HashMap<>();

            data.put(
                    "requests",
                    requestCounts.getOrDefault(client, 0.0)
            );

            data.put(
                    "blocked",
                    blockedCounts.getOrDefault(client, 0.0)
            );

            result.put(client, data);
        }

        return result;
    }
}