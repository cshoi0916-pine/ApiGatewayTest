package com.example.apigateway.registry;

import com.example.apigateway.console.dto.InstanceRequest;
import com.example.apigateway.console.dto.ServiceRequest;
import com.example.apigateway.exception.GatewayServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ServiceRegistryService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryService.class);
    private static final String CACHE_PREFIX = "registry:instances:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final ServiceRepository serviceRepository;
    private final ServiceRegistryRepository repository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ServiceRegistryService(ServiceRepository serviceRepository,
                                  ServiceRegistryRepository repository,
                                  ReactiveStringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this.serviceRepository = serviceRepository;
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ─── 서비스 (논리) ────────────────────────────────────────

    public Flux<ServiceEntity> findAllServices() {
        return serviceRepository.findAllByOrderByCreatedAtDesc();
    }

    public Mono<ServiceEntity> createService(ServiceRequest req) {
        ServiceEntity entity = new ServiceEntity();
        entity.setServiceId(req.getServiceId());
        entity.markNew();
        entity.setServiceName(req.getServiceName());
        entity.setDescription(req.getDescription());
        entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        entity.setCreatedAt(LocalDateTime.now());
        return serviceRepository.save(entity)
            .doOnSuccess(e -> log.info("[Registry] 서비스 생성: {}", e.getServiceId()));
    }

    public Mono<ServiceEntity> updateService(String serviceId, ServiceRequest req) {
        return serviceRepository.findById(serviceId)
            .flatMap(entity -> {
                entity.setServiceName(req.getServiceName());
                entity.setDescription(req.getDescription());
                if (req.getEnabled() != null) entity.setEnabled(req.getEnabled());
                return serviceRepository.save(entity);
            })
            .doOnSuccess(e -> invalidateCache(serviceId));
    }

    public Mono<Void> deleteService(String serviceId) {
        return serviceRepository.deleteById(serviceId)
            .doOnSuccess(v -> {
                invalidateCache(serviceId);
                log.info("[Registry] 서비스 삭제: {}", serviceId);
            });
    }

    // ─── 인스턴스 (물리) ─────────────────────────────────────

    public Flux<ServiceRegistryEntity> findInstancesByServiceId(String serviceId) {
        return repository.findByServiceIdOrderByIdAsc(serviceId);
    }

    public Mono<ServiceRegistryEntity> addInstance(String serviceId, InstanceRequest req) {
        ServiceRegistryEntity entity = new ServiceRegistryEntity();
        entity.setServiceId(serviceId);
        entity.setHost(req.getHost());
        entity.setPort(req.getPort());
        entity.setProtocol(req.getProtocol() != null ? req.getProtocol() : "HTTP");
        entity.setStatus("UP");
        entity.setHealthFailCount(0);
        entity.setLastHeartbeat(LocalDateTime.now());
        entity.setRegisteredAt(LocalDateTime.now());
        entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        entity.setRegistrationType("STATIC");
        return repository.save(entity)
            .doOnSuccess(e -> {
                invalidateCache(serviceId);
                log.info("[Registry] 인스턴스 추가: {}@{}:{}", serviceId, req.getHost(), req.getPort());
            });
    }

    public Mono<ServiceRegistryEntity> updateInstance(Long id, InstanceRequest req) {
        return repository.findById(id)
            .flatMap(entity -> {
                entity.setHost(req.getHost());
                entity.setPort(req.getPort());
                if (req.getProtocol() != null) entity.setProtocol(req.getProtocol());
                if (req.getEnabled() != null) entity.setEnabled(req.getEnabled());
                return repository.save(entity);
            })
            .doOnSuccess(e -> invalidateCache(e.getServiceId()));
    }

    public Mono<Void> deleteInstance(Long id) {
        return repository.findById(id)
            .flatMap(entity -> {
                String serviceId = entity.getServiceId();
                return repository.deleteById(id)
                    .then(Mono.fromRunnable(() -> invalidateCache(serviceId)));
            });
    }

    // ─── 자동 등록 (/registry API) ───────────────────────────

    public Mono<ServiceRegistryEntity> register(String serviceId, String host, Integer port, String protocol) {
        // 서비스가 없으면 자동 생성
        return serviceRepository.findById(serviceId)
            .switchIfEmpty(Mono.defer(() -> {
                ServiceEntity svc = new ServiceEntity();
                svc.setServiceId(serviceId);
                svc.markNew();
                svc.setServiceName(serviceId);
                svc.setEnabled(true);
                svc.setCreatedAt(LocalDateTime.now());
                return serviceRepository.save(svc);
            }))
            .then(
                repository.findByServiceIdAndHostAndPort(serviceId, host, port)
                    .switchIfEmpty(Mono.defer(() -> {
                        ServiceRegistryEntity entity = new ServiceRegistryEntity();
                        entity.setServiceId(serviceId);
                        entity.setHost(host);
                        entity.setPort(port);
                        entity.setProtocol(protocol != null ? protocol : "HTTP");
                        entity.setStatus("UP");
                        entity.setHealthFailCount(0);
                        entity.setLastHeartbeat(LocalDateTime.now());
                        entity.setRegisteredAt(LocalDateTime.now());
                        entity.setEnabled(true);
                        entity.setRegistrationType("DYNAMIC");
                        return repository.save(entity);
                    }))
                    .flatMap(entity -> {
                        entity.setStatus("UP");
                        entity.setHealthFailCount(0);
                        entity.setLastHeartbeat(LocalDateTime.now());
                        entity.setRegistrationType("DYNAMIC");
                        return repository.save(entity);
                    })
                    .doOnSuccess(e -> {
                        invalidateCache(serviceId);
                        log.info("[Registry] 자동등록: {}@{}:{}", serviceId, host, port);
                    })
            );
    }

    public Mono<Void> deregister(String serviceId, String host, Integer port) {
        return repository.findByServiceIdAndHostAndPort(serviceId, host, port)
            .flatMap(entity -> repository.deleteById(entity.getId())
                .then(Mono.fromRunnable(() -> {
                    invalidateCache(serviceId);
                    log.info("[Registry] DYNAMIC 인스턴스 해제 → 삭제: {}@{}:{}", serviceId, host, port);
                })))
            .then();
    }

    public Mono<Void> heartbeat(String serviceId, String host, Integer port) {
        return repository.updateHeartbeat(serviceId, host, port, LocalDateTime.now()).then();
    }

    // ─── LB 용 ───────────────────────────────────────────────

    public Flux<ServiceRegistryEntity> getInstances(String serviceId) {
        String cacheKey = CACHE_PREFIX + serviceId;
        return redisTemplate.opsForValue().get(cacheKey)
            .flatMapMany(json -> {
                try {
                    List<ServiceRegistryEntity> list = objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ServiceRegistryEntity.class));
                    log.debug("[Registry] Cache HIT: serviceId={} instances={}", serviceId, list.size());
                    return Flux.fromIterable(list);
                } catch (JsonProcessingException e) {
                    log.warn("[Registry] 캐시 역직렬화 실패, DB 폴백: {}", e.getMessage());
                    return Flux.empty();
                }
            })
            .switchIfEmpty(
                serviceRepository.findById(serviceId)
                    .switchIfEmpty(Mono.error(new GatewayServiceException(GatewayServiceException.Reason.NOT_FOUND, serviceId)))
                    .flatMapMany(svc -> {
                        if (!Boolean.TRUE.equals(svc.getEnabled())) {
                            return Flux.error(new GatewayServiceException(GatewayServiceException.Reason.SERVICE_DISABLED, serviceId));
                        }
                        return repository.findByServiceIdAndEnabledTrue(serviceId)
                            .collectList()
                            .flatMapMany(allEnabled -> {
                                if (allEnabled.isEmpty()) {
                                    return Flux.error(new GatewayServiceException(GatewayServiceException.Reason.NO_INSTANCES, serviceId));
                                }
                                List<ServiceRegistryEntity> upList = allEnabled.stream()
                                    .filter(e -> "UP".equals(e.getStatus()))
                                    .toList();
                                if (upList.isEmpty()) {
                                    return Flux.error(new GatewayServiceException(GatewayServiceException.Reason.ALL_DOWN, serviceId));
                                }
                                try {
                                    String json = objectMapper.writeValueAsString(upList);
                                    redisTemplate.opsForValue()
                                        .set(cacheKey, json, CACHE_TTL)
                                        .doOnSuccess(v -> log.debug("[Registry] Cache MISS → 저장: serviceId={}", serviceId))
                                        .subscribe();
                                } catch (JsonProcessingException e) {
                                    log.warn("[Registry] 캐시 저장 실패: {}", e.getMessage());
                                }
                                return Flux.fromIterable(upList);
                            });
                    })
            );
    }

    public Mono<Void> evictCache(String serviceId) {
        return redisTemplate.delete(CACHE_PREFIX + serviceId)
            .doOnSuccess(v -> log.info("[Registry] 캐시 강제 무효화: serviceId={}", serviceId))
            .then();
    }

    // ─── 헬스체크 ────────────────────────────────────────────

    public Flux<ServiceRegistryEntity> findAll() {
        return repository.findByEnabledTrue();
    }

    public Mono<Void> updateHealthStatus(Long id, boolean healthy, int currentFailCount) {
        String status = healthy ? "UP" : (currentFailCount >= 3 ? "DOWN" : "CHECKING");
        int failCount = healthy ? 0 : currentFailCount + 1;

        // DOWN 판정 시 DYNAMIC 인스턴스는 삭제
        if (!healthy && "DOWN".equals(status)) {
            return repository.findById(id)
                .flatMap(entity -> {
                    if ("DYNAMIC".equals(entity.getRegistrationType())) {
                        log.info("[HealthCheck] DYNAMIC 인스턴스 자동 삭제: {}@{}:{}", entity.getServiceId(), entity.getHost(), entity.getPort());
                        return repository.deleteById(id)
                            .then(Mono.fromRunnable(() -> invalidateCache(entity.getServiceId())));
                    }
                    return repository.updateStatus(id, status, failCount)
                        .flatMap(rows -> repository.findById(id))
                        .doOnSuccess(e -> { if (e != null) invalidateCache(e.getServiceId()); })
                        .then();
                });
        }

        Mono<Integer> updateMono = healthy
            ? repository.updateStatusAndHeartbeat(id, status, failCount, LocalDateTime.now())
            : repository.updateStatus(id, status, failCount);

        return updateMono
            .flatMap(rows -> repository.findById(id))
            .doOnSuccess(e -> {
                if (e != null) invalidateCache(e.getServiceId());
            })
            .then();
    }

    public Flux<ServiceRegistryEntity> findStale(LocalDateTime threshold) {
        return repository.findStaleServices(threshold);
    }

    public Flux<ServiceRegistryEntity> findStaleDynamic(LocalDateTime threshold) {
        return repository.findStaleDynamicInstances(threshold);
    }

    // ─── 내부 ────────────────────────────────────────────────

    private void invalidateCache(String serviceId) {
        redisTemplate.delete(CACHE_PREFIX + serviceId)
            .doOnSuccess(v -> log.debug("[Registry] 캐시 무효화: serviceId={}", serviceId))
            .subscribe();
    }
}
