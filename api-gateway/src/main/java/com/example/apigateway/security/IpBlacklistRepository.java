package com.example.apigateway.security;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IpBlacklistRepository extends ReactiveCrudRepository<IpBlacklistEntity, Long> {
    Flux<IpBlacklistEntity> findAllByOrderByCreatedAtDesc();

    // 만료되지 않은 IP만 조회 (캐시 갱신 시 사용)
    @Query("SELECT * FROM gateway.ip_blacklist WHERE expired_at IS NULL OR expired_at > NOW()")
    Flux<IpBlacklistEntity> findAllActive();

    Mono<IpBlacklistEntity> findByIpAddress(String ipAddress);
}
