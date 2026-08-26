package com.example.apigateway.registry;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ServiceRegistryRepository extends ReactiveCrudRepository<ServiceRegistryEntity, Long> {

    Flux<ServiceRegistryEntity> findByEnabledTrue();

    Flux<ServiceRegistryEntity> findByServiceIdAndEnabledTrue(String serviceId);

    Mono<ServiceRegistryEntity> findByServiceIdAndHostAndPort(String serviceId, String host, Integer port);

    Flux<ServiceRegistryEntity> findByServiceIdOrderByIdAsc(String serviceId);

    @Query("SELECT * FROM gateway.service_instance WHERE enabled = true AND last_heartbeat < :threshold AND status = 'UP'")
    Flux<ServiceRegistryEntity> findStaleServices(LocalDateTime threshold);

    @Query("SELECT * FROM gateway.service_instance WHERE registration_type = 'DYNAMIC' AND last_heartbeat < :threshold")
    Flux<ServiceRegistryEntity> findStaleDynamicInstances(LocalDateTime threshold);

    @Modifying
    @Query("UPDATE gateway.service_instance SET last_heartbeat = :now WHERE service_id = :serviceId AND host = :host AND port = :port")
    Mono<Integer> updateHeartbeat(String serviceId, String host, Integer port, LocalDateTime now);

    @Modifying
    @Query("UPDATE gateway.service_instance SET status = :status, health_fail_count = :failCount WHERE id = :id")
    Mono<Integer> updateStatus(Long id, String status, Integer failCount);

    @Modifying
    @Query("UPDATE gateway.service_instance SET status = :status, health_fail_count = :failCount, last_heartbeat = :now WHERE id = :id")
    Mono<Integer> updateStatusAndHeartbeat(Long id, String status, Integer failCount, LocalDateTime now);
}
