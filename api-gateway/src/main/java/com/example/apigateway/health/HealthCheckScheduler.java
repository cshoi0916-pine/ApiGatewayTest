package com.example.apigateway.health;

import com.example.apigateway.registry.ServiceRegistryEntity;
import com.example.apigateway.registry.ServiceRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;

@Component
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final ServiceRegistryService registryService;
    private final WebClient webClient;

    @Value("${health-check.heartbeat-timeout-seconds:30}")
    private int heartbeatTimeout;

    @Value("${health-check.fail-threshold:3}")
    private int failThreshold;

    public HealthCheckScheduler(ServiceRegistryService registryService,
                                WebClient.Builder webClientBuilder) {
        this.registryService = registryService;
        this.webClient = webClientBuilder.build();
    }

    // Heartbeat 미수신 서비스 DOWN 처리 (30초마다)
    @Scheduled(fixedDelay = 30000)
    public void checkStaleHeartbeats() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeout);
        registryService.findStale(threshold)
            .flatMap(entity -> {
                log.warn("[HealthCheck] Heartbeat 미수신 → DOWN: {}@{}:{}", entity.getServiceId(), entity.getHost(), entity.getPort());
                return registryService.updateHealthStatus(entity.getId(), false, failThreshold);
            })
            .subscribe();
    }

    // DYNAMIC 인스턴스 Heartbeat 만료 시 자동 삭제 (30초마다)
    @Scheduled(fixedDelay = 30000)
    public void cleanupStaleDynamicInstances() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeout);
        registryService.findStaleDynamic(threshold)
            .flatMap(entity -> {
                log.info("[Registry] DYNAMIC 인스턴스 Heartbeat 만료 → 자동 삭제: {}@{}:{}",
                    entity.getServiceId(), entity.getHost(), entity.getPort());
                return registryService.deleteInstance(
                        entity.getId());
            })
            .subscribe();
    }

    // Active 헬스체크: /actuator/health 호출 (15초마다)
    @Scheduled(fixedDelay = 15000)
    public void activeHealthCheck() {
        registryService.findAll()
            .flatMap(entity -> checkHealth(entity))
            .subscribe();
    }

    private Mono<Void> checkHealth(ServiceRegistryEntity entity) {
        String scheme = "HTTPS".equalsIgnoreCase(entity.getProtocol()) ? "https" : "http";
        String url = scheme + "://" + entity.getHost() + ":" + entity.getPort() + "/actuator/health";
        int currentFail = entity.getHealthFailCount() != null ? entity.getHealthFailCount() : 0;

        return webClient.get()
            .uri(url)
            .exchangeToMono(response -> {
                if (response.statusCode().value() == 200) {
                    // Spring Boot actuator: status 필드로 판단
                    return response.bodyToMono(Map.class)
                        .map(body -> "UP".equals(body.get("status")))
                        .onErrorReturn(true)   // JSON 파싱 실패(HTML 등) → 서버 살아있음
                        .defaultIfEmpty(true); // 빈 body → 서버 살아있음
                }
                // 404, 403, 302, 5xx 등 → HTTP 응답 자체가 온 것 = 서버 살아있음
                return response.releaseBody().thenReturn(true);
            })
            .timeout(Duration.ofSeconds(5))
            .flatMap(up -> registryService.updateHealthStatus(entity.getId(), up, up ? 0 : currentFail))
            .onErrorResume(e -> {
                // 연결 자체 실패 (Connection refused, timeout 등) → 진짜 DOWN
                log.debug("[HealthCheck] 연결 실패: {}@{}:{} ({})", entity.getServiceId(), entity.getHost(), entity.getPort(), e.getMessage());
                return registryService.updateHealthStatus(entity.getId(), false, currentFail);
            });
    }
}
