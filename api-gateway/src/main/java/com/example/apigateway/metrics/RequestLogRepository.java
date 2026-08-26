package com.example.apigateway.metrics;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RequestLogRepository extends ReactiveCrudRepository<RequestLogEntity, Long> {

    Flux<RequestLogEntity> findTop100ByOrderByCreatedAtDesc();
    Flux<RequestLogEntity> findTop100ByServiceIdOrderByCreatedAtDesc(String serviceId);

    @Query("SELECT * FROM gateway.request_log ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<RequestLogEntity> findPageOrderByCreatedAtDesc(int limit, long offset);

    @Query("SELECT * FROM gateway.request_log WHERE service_id = :serviceId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<RequestLogEntity> findPageByServiceIdOrderByCreatedAtDesc(String serviceId, int limit, long offset);

    @Query("SELECT COUNT(*) FROM gateway.request_log WHERE service_id = :serviceId")
    Mono<Long> countByServiceId(String serviceId);
}
