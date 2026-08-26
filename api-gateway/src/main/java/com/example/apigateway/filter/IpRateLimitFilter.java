package com.example.apigateway.filter;

import com.example.apigateway.metrics.ApiMetricsService;
import com.example.apigateway.security.IpBlacklistEntity;
import com.example.apigateway.security.IpBlacklistFilter;
import com.example.apigateway.security.IpBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class IpRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IpRateLimitFilter.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiMetricsService metricsService;
    private final IpBlacklistRepository blacklistRepository;
    private final IpBlacklistFilter blacklistFilter;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.requests-per-minute:300}")
    private int requestsPerMinute;

    @Value("${rate-limit.trusted-proxies:}")
    private List<String> trustedProxies;

    @Value("${rate-limit.auto-blacklist.enabled:true}")
    private boolean autoBlacklistEnabled;

    @Value("${rate-limit.auto-blacklist.exceed-count:5}")
    private int exceedCount;

    @Value("${rate-limit.auto-blacklist.ttl-hours:1}")
    private int ttlHours;

    @Value("${rate-limit.whitelist-prefixes:/console,/actuator,/registry}")
    private List<String> whitelistPrefixes;

    public IpRateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
                             ApiMetricsService metricsService,
                             IpBlacklistRepository blacklistRepository,
                             IpBlacklistFilter blacklistFilter) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
        this.blacklistRepository = blacklistRepository;
        this.blacklistFilter = blacklistFilter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        if (!enabled) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (whitelistPrefixes.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String ip = getClientIp(exchange);
        String key = "rate:" + ip;

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    if (count == 1) {
                        redisTemplate.expire(key, Duration.ofSeconds(60)).subscribe();
                    }

                    if (count > requestsPerMinute) {
                        log.warn("[RATE-LIMIT] blocked ip={}, count={}/{}", ip, count, requestsPerMinute);
                        metricsService.recordBlocked(ip);
                        if (autoBlacklistEnabled) {
                            tryAutoBlacklist(ip, count);
                        }
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }

                    metricsService.recordRequest(ip);
                    return chain.filter(exchange);
                });
    }

    private void tryAutoBlacklist(String ip, long currentCount) {
        // Redis에 연속 초과 카운트 기록 (키 만료 = 2분, 한 번이라도 정상이면 자연 소멸)
        String exceedKey = "rate:exceed:" + ip;
        redisTemplate.opsForValue().increment(exceedKey)
            .flatMap(exceedCnt -> {
                if (exceedCnt == 1) {
                    return redisTemplate.expire(exceedKey, Duration.ofMinutes(exceedCount * 2L));
                }
                if (exceedCnt >= exceedCount) {
                    return blacklistRepository.findByIpAddress(ip)
                        .switchIfEmpty(Mono.defer(() -> {
                            IpBlacklistEntity entity = new IpBlacklistEntity();
                            entity.setIpAddress(ip);
                            entity.setReason("Rate Limit 자동 차단 (분당 " + requestsPerMinute + "회 초과 " + exceedCount + "회 연속)");
                            entity.setCreatedAt(LocalDateTime.now());
                            entity.setExpiredAt(LocalDateTime.now().plusHours(ttlHours));
                            entity.setAuto(true);
                            return blacklistRepository.save(entity)
                                .doOnNext(saved -> {
                                    blacklistFilter.refreshCache();
                                    redisTemplate.delete(exceedKey).subscribe();
                                    log.warn("[RATE-LIMIT] 자동 블랙리스트 등록: ip={}, {}시간 후 해제", ip, ttlHours);
                                });
                        }))
                        .then();
                }
                return Mono.empty();
            })
            .subscribe();
    }

    private String getClientIp(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        String remoteIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";

        // X-Forwarded-For는 신뢰할 수 있는 프록시에서 온 요청일 때만 사용
        // 직접 연결된 클라이언트가 이 헤더를 위조해서 Rate Limit을 우회하는 것을 방지
        if (!trustedProxies.isEmpty() && isTrustedProxy(remoteIp)) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }

        return remoteIp;
    }

    private boolean isTrustedProxy(String ip) {
        for (String trusted : trustedProxies) {
            if (trusted.contains("/")) {
                if (isInCidrRange(ip, trusted)) return true;
            } else {
                if (trusted.equals(ip)) return true;
            }
        }
        return false;
    }

    private boolean isInCidrRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefixLen = Integer.parseInt(parts[1]);
            byte[] networkBytes = InetAddress.getByName(parts[0]).getAddress();
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            if (networkBytes.length != ipBytes.length) return false;
            int mask = prefixLen == 0 ? 0 : (0xFFFFFFFF << (32 - prefixLen));
            int network = ((networkBytes[0] & 0xFF) << 24) | ((networkBytes[1] & 0xFF) << 16)
                        | ((networkBytes[2] & 0xFF) << 8)  |  (networkBytes[3] & 0xFF);
            int target  = ((ipBytes[0] & 0xFF) << 24) | ((ipBytes[1] & 0xFF) << 16)
                        | ((ipBytes[2] & 0xFF) << 8)  |  (ipBytes[3] & 0xFF);
            return (network & mask) == (target & mask);
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
