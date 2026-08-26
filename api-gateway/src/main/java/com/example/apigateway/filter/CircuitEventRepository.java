package com.example.apigateway.filter;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CircuitEventRepository extends ReactiveCrudRepository<CircuitEventEntity, Long> {

    Flux<CircuitEventEntity> findTop200ByOrderByEventAtDesc();
}
