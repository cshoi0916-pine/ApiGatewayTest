package com.example.apigateway.route;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RouteRepository extends ReactiveCrudRepository<RouteEntity, String> {

    Flux<RouteEntity> findByEnabledTrueOrderByPriorityDesc();

    Flux<RouteEntity> findAllByOrderByPriorityDesc();

    Flux<RouteEntity> findAllByOrderByCreatedAtAsc();
}
