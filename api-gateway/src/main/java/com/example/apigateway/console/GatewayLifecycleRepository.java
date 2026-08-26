package com.example.apigateway.console;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface GatewayLifecycleRepository extends ReactiveCrudRepository<GatewayLifecycleEntity, Long> {
    Flux<GatewayLifecycleEntity> findTop50ByOrderByEventAtDesc();
}
