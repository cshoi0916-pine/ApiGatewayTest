package com.example.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpBlacklistFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(IpBlacklistFilter.class);

    private final IpBlacklistRepository repo;
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public IpBlacklistFilter(IpBlacklistRepository repo) {
        this.repo = repo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        refreshCache();
    }

    public void refreshCache() {
        // 만료되지 않은 항목만 캐시에 올림
        repo.findAllActive()
            .map(IpBlacklistEntity::getIpAddress)
            .collectList()
            .subscribe(list -> {
                blacklist.clear();
                blacklist.addAll(list);
                log.info("[Blacklist] 캐시 갱신: {}개", list.size());
            });
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/console") || path.startsWith("/auth")
                || path.startsWith("/actuator") || path.startsWith("/registry")) {
            return chain.filter(exchange);
        }
        String ip = getClientIp(exchange);
        if (blacklist.contains(ip)) {
            log.warn("[Blacklist] 차단: ip={}, path={}", ip, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private String getClientIp(ServerWebExchange exchange) {
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() { return -100; }
}
