package com.example.apigateway.registry;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ServiceRepository extends ReactiveCrudRepository<ServiceEntity, String> {
    Flux<ServiceEntity> findAllByOrderByCreatedAtDesc();
}
