package com.example.apigateway.route;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RouteConditionRepository extends ReactiveCrudRepository<RouteConditionEntity, Long> {

    Flux<RouteConditionEntity> findByRouteId(String routeId);

    @Modifying
    @Query("DELETE FROM gateway.route_condition WHERE route_id = :routeId")
    Mono<Integer> deleteByRouteId(String routeId);
}
