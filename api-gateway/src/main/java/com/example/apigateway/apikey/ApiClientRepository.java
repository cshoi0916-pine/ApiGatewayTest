package com.example.apigateway.apikey;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ApiClientRepository extends ReactiveCrudRepository<ApiClient, Long> {

    @Query("SELECT * FROM gateway.api_client WHERE api_key = :apiKey AND enabled = true AND (expired_at IS NULL OR expired_at > NOW())")
    Mono<ApiClient> findValidByApiKey(String apiKey);
}
