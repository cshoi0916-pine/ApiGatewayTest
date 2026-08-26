package com.example.apigateway.console;

import com.example.apigateway.console.dto.InstanceRequest;
import com.example.apigateway.console.dto.RouteRequest;
import com.example.apigateway.console.dto.ServiceRequest;
import com.example.apigateway.filter.CircuitEventRepository;
import com.example.apigateway.filter.SimpleCircuitBreakerFilter;
import com.example.apigateway.metrics.ApiMetricsService;
import com.example.apigateway.auth.LoginLogRepository;
import com.example.apigateway.metrics.RequestLogRepository;
import com.example.apigateway.registry.ServiceRegistryService;
import com.example.apigateway.security.IpBlacklistEntity;
import com.example.apigateway.security.IpBlacklistFilter;
import com.example.apigateway.security.IpBlacklistRepository;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/console/api")
public class ConsoleController {

    private final ServiceRegistryService registryService;
    private final DynamicRouteService dynamicRouteService;
    private final ApiMetricsService metricsService;
    private final RequestLogRepository logRepository;
    private final GatewayLifecycleService lifecycleService;
    private final GatewayLifecycleRepository lifecycleRepository;
    private final LoginLogRepository loginLogRepository;
    private final SimpleCircuitBreakerFilter circuitBreakerFilter;
    private final IpBlacklistRepository ipBlacklistRepository;
    private final IpBlacklistFilter ipBlacklistFilter;
    private final DatabaseClient databaseClient;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final CircuitEventRepository circuitEventRepository;

    private static final String BRUTE_FORCE_KEY_PREFIX = "login:fail:";
    private static final String RATE_KEY_PREFIX = "rate:";

    public ConsoleController(ServiceRegistryService registryService,
                             DynamicRouteService dynamicRouteService,
                             ApiMetricsService metricsService,
                             RequestLogRepository logRepository,
                             GatewayLifecycleService lifecycleService,
                             GatewayLifecycleRepository lifecycleRepository,
                             LoginLogRepository loginLogRepository,
                             SimpleCircuitBreakerFilter circuitBreakerFilter,
                             IpBlacklistRepository ipBlacklistRepository,
                             IpBlacklistFilter ipBlacklistFilter,
                             DatabaseClient databaseClient,
                             ReactiveStringRedisTemplate redisTemplate,
                             CircuitEventRepository circuitEventRepository) {
        this.registryService = registryService;
        this.dynamicRouteService = dynamicRouteService;
        this.metricsService = metricsService;
        this.logRepository = logRepository;
        this.lifecycleService = lifecycleService;
        this.lifecycleRepository = lifecycleRepository;
        this.loginLogRepository = loginLogRepository;
        this.circuitBreakerFilter = circuitBreakerFilter;
        this.ipBlacklistRepository = ipBlacklistRepository;
        this.ipBlacklistFilter = ipBlacklistFilter;
        this.databaseClient = databaseClient;
        this.redisTemplate = redisTemplate;
        this.circuitEventRepository = circuitEventRepository;
    }

    // ─── 서비스 (논리) ────────────────────────────────────────

    @GetMapping("/services")
    public Mono<List<Map<String, Object>>> getServices() {
        return registryService.findAllServices()
            .flatMap(svc ->
                registryService.findInstancesByServiceId(svc.getServiceId())
                    .collectList()
                    .map(instances -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("serviceId", svc.getServiceId());
                        m.put("serviceName", svc.getServiceName());
                        m.put("description", svc.getDescription());
                        m.put("enabled", svc.getEnabled());
                        m.put("createdAt", svc.getCreatedAt());
                        m.put("instances", instances.stream().map(i -> {
                            Map<String, Object> im = new LinkedHashMap<>();
                            im.put("id", i.getId());
                            im.put("host", i.getHost());
                            im.put("port", i.getPort());
                            im.put("protocol", i.getProtocol());
                            im.put("status", i.getStatus());
                            im.put("healthFailCount", i.getHealthFailCount());
                            im.put("lastHeartbeat", i.getLastHeartbeat());
                            im.put("enabled", i.getEnabled());
                            im.put("registrationType", i.getRegistrationType() != null ? i.getRegistrationType() : "STATIC");
                            return im;
                        }).toList());
                        return m;
                    })
            )
            .collectList();
    }

    @PostMapping("/services")
    public Mono<ResponseEntity<Map<String, Object>>> addService(@RequestBody ServiceRequest request) {
        return registryService.createService(request)
            .map(e -> ResponseEntity.ok(Map.of("result", "created", "serviceId", e.getServiceId())));
    }

    @PutMapping("/services/{serviceId}")
    public Mono<ResponseEntity<Map<String, Object>>> updateService(@PathVariable String serviceId,
                                                                    @RequestBody ServiceRequest request) {
        return registryService.updateService(serviceId, request)
            .map(e -> ResponseEntity.ok(Map.of("result", "updated", "serviceId", e.getServiceId())));
    }

    @DeleteMapping("/services/{serviceId}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteService(@PathVariable String serviceId) {
        return registryService.deleteService(serviceId)
            .thenReturn(ResponseEntity.ok(Map.of("result", "deleted", "serviceId", serviceId)));
    }

    // ─── 인스턴스 (물리) ─────────────────────────────────────

    @PostMapping("/services/{serviceId}/instances")
    public Mono<ResponseEntity<Map<String, Object>>> addInstance(@PathVariable String serviceId,
                                                                  @RequestBody InstanceRequest request) {
        return registryService.addInstance(serviceId, request)
            .map(e -> ResponseEntity.ok(Map.of("result", "added", "id", e.getId())));
    }

    @PutMapping("/services/{serviceId}/instances/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateInstance(@PathVariable String serviceId,
                                                                     @PathVariable Long id,
                                                                     @RequestBody InstanceRequest request) {
        return registryService.updateInstance(id, request)
            .map(e -> ResponseEntity.ok(Map.of("result", "updated", "id", e.getId())));
    }

    @DeleteMapping("/services/{serviceId}/instances/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteInstance(@PathVariable String serviceId,
                                                                     @PathVariable Long id) {
        return registryService.deleteInstance(id)
            .thenReturn(ResponseEntity.ok(Map.of("result", "deleted", "id", id)));
    }

    // ─── 라우팅 룰 ───────────────────────────────────────────

    @GetMapping("/routes")
    public Mono<List<Map<String, Object>>> getRoutes() {
        return dynamicRouteService.findAllRoutes()
            .flatMap(entity -> dynamicRouteService.findConditions(entity.getId())
                .map(conditions -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", entity.getId());
                    m.put("name", entity.getName());
                    m.put("targetServiceId", entity.getTargetServiceId());
                    m.put("targetPort", entity.getTargetPort());
                    m.put("method", entity.getMethod());
                    m.put("apiPath", entity.getApiPath());
                    m.put("priority", entity.getPriority());
                    m.put("description", entity.getDescription());
                    m.put("routeType", entity.getRouteType());
                    m.put("stripPrefix", entity.getStripPrefix());
                    m.put("timeoutMs", entity.getTimeoutMs());
                    m.put("retryCount", entity.getRetryCount());
                    m.put("enabled", entity.getEnabled());
                    m.put("createdAt", entity.getCreatedAt());
                    m.put("conditions", conditions.stream().map(c -> {
                        Map<String, Object> cm = new LinkedHashMap<>();
                        cm.put("id", c.getId());
                        cm.put("type", c.getType());
                        cm.put("operation", c.getOperation());
                        cm.put("attributeName", c.getAttributeName());
                        cm.put("attributeValue", c.getAttributeValue());
                        return cm;
                    }).toList());
                    return m;
                })
            )
            .collectList();
    }

    @PostMapping("/routes")
    public Mono<ResponseEntity<Map<String, Object>>> addRoute(@RequestBody RouteRequest request) {
        return dynamicRouteService.addRoute(request)
            .map(e -> ResponseEntity.ok(Map.of("result", "added", "id", e.getId())));
    }

    @PutMapping("/routes/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateRoute(@PathVariable String id,
                                                                  @RequestBody RouteRequest request) {
        request.setId(id);
        return dynamicRouteService.updateRoute(id, request)
            .map(e -> ResponseEntity.ok(Map.of("result", "updated", "id", e.getId())));
    }

    @DeleteMapping("/routes/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteRoute(@PathVariable String id) {
        return dynamicRouteService.deleteRoute(id)
            .thenReturn(ResponseEntity.ok(Map.of("result", "deleted", "id", id)));
    }

    // ─── 요청 이력 ───────────────────────────────────────────

    @GetMapping("/logs")
    public Mono<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "") String serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        long offset = (long) page * size;
        var flux = serviceId.isBlank()
            ? logRepository.findPageOrderByCreatedAtDesc(size, offset)
            : logRepository.findPageByServiceIdOrderByCreatedAtDesc(serviceId, size, offset);
        var countMono = serviceId.isBlank()
            ? logRepository.count()
            : logRepository.countByServiceId(serviceId);

        return Mono.zip(flux.map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("createdAt", e.getCreatedAt());
            m.put("method", e.getMethod());
            m.put("path", e.getPath());
            m.put("serviceId", e.getServiceId());
            m.put("statusCode", e.getStatusCode());
            m.put("durationMs", e.getDurationMs());
            m.put("clientIp", e.getClientIp());
            m.put("instanceAddress", e.getInstanceAddress());
            return m;
        }).collectList(), countMono).map(tuple -> {
            long total = tuple.getT2();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", tuple.getT1());
            result.put("totalElements", total);
            result.put("totalPages", (int) Math.ceil((double) total / size));
            result.put("page", page);
            result.put("size", size);
            return result;
        });
    }

    // ─── 통계 ────────────────────────────────────────────────

    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return Mono.zip(
            registryService.findAllServices().count(),
            registryService.findAllServices().filter(s -> Boolean.TRUE.equals(s.getEnabled())).count(),
            registryService.findAll().filter(e -> "UP".equals(e.getStatus())).count(),
            registryService.findAll().filter(e -> !"UP".equals(e.getStatus())).count(),
            dynamicRouteService.findAllRoutes().count(),
            dynamicRouteService.findAllRoutes().filter(r -> Boolean.TRUE.equals(r.getEnabled())).count(),
            logRepository.count()
        ).map(tuple -> {
            Map<String, Object> stats = new LinkedHashMap<>(metricsService.getStats());
            stats.put("totalServiceCount",  tuple.getT1());
            stats.put("activeServiceCount", tuple.getT2());
            stats.put("upCount",            tuple.getT3());
            stats.put("downCount",          tuple.getT4());
            stats.put("totalRouteCount",    tuple.getT5());
            stats.put("activeRouteCount",   tuple.getT6());
            stats.put("totalRequests",      tuple.getT7());
            return stats;
        });
    }

    // ─── 게이트웨이 정보 ──────────────────────────────────────

    @GetMapping("/info")
    public Mono<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name",          lifecycleService.getName());
        info.put("environment",   lifecycleService.getEnvironment());
        info.put("startedAt",     lifecycleService.getStartedAt());
        info.put("uptimeSeconds", lifecycleService.getUptimeSeconds());
        return Mono.just(info);
    }

    @GetMapping("/lifecycle")
    public Mono<List<Map<String, Object>>> getLifecycle() {
        return lifecycleRepository.findTop50ByOrderByEventAtDesc()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("event",    e.getEvent());
                m.put("nodeName", e.getNodeName());
                m.put("eventAt",  e.getEventAt());
                return m;
            })
            .collectList();
    }

    // ─── 서킷브레이크 ─────────────────────────────────────────

    @GetMapping("/circuit-breaker")
    public Mono<List<Map<String, Object>>> getCircuitBreakerStatus() {
        return registryService.findAllServices()
            .map(svc -> {
                Map<String, Object> cb = circuitBreakerFilter.getServiceStatus(svc.getServiceId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("serviceId",   svc.getServiceId());
                m.put("serviceName", svc.getServiceName());
                m.putAll(cb);
                return m;
            })
            .collectList();
    }

    // ─── 서킷브레이커 강제 제어 ───────────────────────────────

    @PostMapping("/circuit-breaker/{serviceId}/open")
    public Mono<ResponseEntity<Map<String, Object>>> forceOpen(@PathVariable String serviceId) {
        circuitBreakerFilter.forceOpen(serviceId);
        return Mono.just(ResponseEntity.ok(Map.of("result", "forced_open", "serviceId", serviceId)));
    }

    @PostMapping("/circuit-breaker/{serviceId}/close")
    public Mono<ResponseEntity<Map<String, Object>>> forceClose(@PathVariable String serviceId) {
        circuitBreakerFilter.forceClose(serviceId);
        return Mono.just(ResponseEntity.ok(Map.of("result", "forced_close", "serviceId", serviceId)));
    }

    // ─── IP 블랙리스트 ────────────────────────────────────────

    @GetMapping("/blacklist")
    public Flux<Map<String, Object>> getBlacklist() {
        return ipBlacklistRepository.findAllByOrderByCreatedAtDesc()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        e.getId());
                m.put("ipAddress", e.getIpAddress());
                m.put("reason",    e.getReason());
                m.put("createdAt", e.getCreatedAt());
                m.put("expiredAt", e.getExpiredAt());
                m.put("auto",      e.isAuto());
                return m;
            });
    }

    @PostMapping("/blacklist")
    public Mono<ResponseEntity<Map<String, Object>>> addBlacklist(@RequestBody Map<String, String> body) {
        IpBlacklistEntity entity = new IpBlacklistEntity();
        entity.setIpAddress(body.get("ipAddress"));
        entity.setReason(body.getOrDefault("reason", ""));
        return ipBlacklistRepository.save(entity)
            .doOnSuccess(e -> ipBlacklistFilter.refreshCache())
            .map(e -> ResponseEntity.ok(Map.of("result", "added", "id", e.getId())));
    }

    @DeleteMapping("/blacklist/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteBlacklist(@PathVariable Long id) {
        return ipBlacklistRepository.deleteById(id)
            .doOnSuccess(v -> ipBlacklistFilter.refreshCache())
            .thenReturn(ResponseEntity.ok(Map.of("result", "deleted", "id", id)));
    }

    // ─── 서비스별 요청 통계 ───────────────────────────────────

    @GetMapping("/stats/services")
    public Mono<List<Map<String, Object>>> getServiceStats() {
        return databaseClient.sql(
                "SELECT service_id, COUNT(*) AS total, " +
                "SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END) AS errors, " +
                "ROUND(AVG(duration_ms)) AS avg_duration_ms " +
                "FROM gateway.request_log GROUP BY service_id ORDER BY total DESC")
            .fetch().all()
            .map(row -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("serviceId",     row.get("service_id"));
                m.put("total",         row.get("total"));
                m.put("errors",        row.get("errors"));
                m.put("avgDurationMs", row.get("avg_duration_ms"));
                return m;
            })
            .collectList();
    }

    // ─── 접속 이력 ────────────────────────────────────────────

    @GetMapping("/login-logs")
    public Mono<List<Map<String, Object>>> getLoginLogs() {
        return loginLogRepository.findTop100ByOrderByCreatedAtDesc()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username",  e.getUsername());
                m.put("clientIp",  e.getClientIp());
                m.put("result",    e.getResult());
                m.put("createdAt", e.getCreatedAt());
                return m;
            })
            .collectList();
    }

    // ─── 브루트포스 현황 ──────────────────────────────────────

    @GetMapping("/brute-force")
    public Flux<Map<String, Object>> getBruteForceStatus() {
        return redisTemplate.keys(BRUTE_FORCE_KEY_PREFIX + "*")
            .flatMap(key -> {
                String ip = key.substring(BRUTE_FORCE_KEY_PREFIX.length());
                return Mono.zip(
                    redisTemplate.opsForValue().get(key).defaultIfEmpty("0"),
                    redisTemplate.getExpire(key).defaultIfEmpty(java.time.Duration.ZERO)
                ).map(tuple -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ip",         ip);
                    m.put("failCount",  Integer.parseInt(tuple.getT1()));
                    m.put("ttlSeconds", tuple.getT2().getSeconds());
                    return m;
                });
            })
            .sort((a, b) -> Integer.compare((int) b.get("failCount"), (int) a.get("failCount")));
    }

    @DeleteMapping("/brute-force/{ip}")
    public Mono<ResponseEntity<Map<String, Object>>> resetBruteForce(@PathVariable String ip) {
        return redisTemplate.delete(BRUTE_FORCE_KEY_PREFIX + ip)
            .thenReturn(ResponseEntity.ok(Map.of("result", "reset", "ip", ip)));
    }

    // ─── Rate Limit 현황 ──────────────────────────────────────

    @GetMapping("/rate-limit-status")
    public Flux<Map<String, Object>> getRateLimitStatus() {
        return redisTemplate.keys(RATE_KEY_PREFIX + "*")
            .filter(key -> !key.startsWith("rate:exceed:"))
            .flatMap(key -> {
                String ip = key.substring(RATE_KEY_PREFIX.length());
                return Mono.zip(
                    redisTemplate.opsForValue().get(key).defaultIfEmpty("0"),
                    redisTemplate.getExpire(key).defaultIfEmpty(java.time.Duration.ZERO)
                ).map(tuple -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ip",         ip);
                    m.put("count",      Integer.parseInt(tuple.getT1()));
                    m.put("ttlSeconds", tuple.getT2().getSeconds());
                    return m;
                });
            })
            .sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
    }

    // ─── 서킷브레이커 이벤트 이력 ────────────────────────────

    @GetMapping("/circuit-events")
    public Mono<List<Map<String, Object>>> getCircuitEvents() {
        return circuitEventRepository.findTop200ByOrderByEventAtDesc()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("serviceId", e.getServiceId());
                m.put("event",     e.getEvent());
                m.put("detail",    e.getDetail());
                m.put("eventAt",   e.getEventAt());
                return m;
            })
            .collectList();
    }
}
