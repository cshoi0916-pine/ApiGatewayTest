package com.example.apigateway.console;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class GatewayLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(GatewayLifecycleService.class);

    @Value("${gateway.name:APIGW}")
    private String name;

    @Value("${gateway.environment:DEV}")
    private String environment;

    private LocalDateTime startedAt;

    private final GatewayLifecycleRepository lifecycleRepository;

    public GatewayLifecycleService(GatewayLifecycleRepository lifecycleRepository) {
        this.lifecycleRepository = lifecycleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        startedAt = LocalDateTime.now();
        GatewayLifecycleEntity e = new GatewayLifecycleEntity();
        e.setEvent("START");
        e.setNodeName(name);
        e.setEventAt(startedAt);
        lifecycleRepository.save(e).subscribe();
        log.info("[Gateway] 시작: name={}, env={}", name, environment);
    }

    @PreDestroy
    public void onShutdown() {
        GatewayLifecycleEntity e = new GatewayLifecycleEntity();
        e.setEvent("STOP");
        e.setNodeName(name);
        e.setEventAt(LocalDateTime.now());
        try {
            lifecycleRepository.save(e).block(Duration.ofSeconds(3));
            log.info("[Gateway] 종료 기록 완료");
        } catch (Exception ex) {
            log.warn("[Gateway] 종료 기록 실패: {}", ex.getMessage());
        }
    }

    public String getName() { return name; }
    public String getEnvironment() { return environment; }
    public LocalDateTime getStartedAt() { return startedAt; }

    public long getUptimeSeconds() {
        if (startedAt == null) return 0;
        return Duration.between(startedAt, LocalDateTime.now()).getSeconds();
    }
}
