package com.example.apigateway.auth;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface LoginLogRepository extends ReactiveCrudRepository<LoginLogEntity, Long> {
    Flux<LoginLogEntity> findTop100ByOrderByCreatedAtDesc();
}
