package com.example.apigateway.console;

import com.example.apigateway.console.dto.RouteRequest;
import com.example.apigateway.route.RouteConditionEntity;
import com.example.apigateway.route.RouteConditionRepository;
import com.example.apigateway.route.RouteEntity;
import com.example.apigateway.route.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicRouteService implements ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(DynamicRouteService.class);

    private final RouteDefinitionWriter routeDefinitionWriter;
    private final RouteRepository routeRepository;
    private final RouteConditionRepository conditionRepository;
    private ApplicationEventPublisher publisher;

    private final Set<String> manualRouteIds = ConcurrentHashMap.newKeySet();

    public DynamicRouteService(RouteDefinitionWriter routeDefinitionWriter,
                               RouteRepository routeRepository,
                               RouteConditionRepository conditionRepository) {
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.routeRepository = routeRepository;
        this.conditionRepository = conditionRepository;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    // 기동 시 DB에서 라우트 복원
    @EventListener(ApplicationReadyEvent.class)
    public void restoreRoutesFromDb() {
        log.info("[Route] DB에서 라우트 복원 시작");
        routeRepository.findByEnabledTrueOrderByPriorityDesc()
            .flatMap(entity -> conditionRepository.findByRouteId(entity.getId())
                .collectList()
                .map(conditions -> buildDefinition(entity, conditions))
                .flatMap(def -> routeDefinitionWriter.save(Mono.just(def)))
                .doOnSuccess(v -> {
                    manualRouteIds.add(entity.getId());
                    log.info("[Route] 복원: id={}", entity.getId());
                })
            )
            .collectList()
            .subscribe(list -> {
                publisher.publishEvent(new RefreshRoutesEvent(this));
                log.info("[Route] 복원 완료 ({}개)", list.size());
            });
    }

    public Mono<RouteEntity> addRoute(RouteRequest request) {
        RouteEntity entity = new RouteEntity();
        String id = (request.getId() != null && !request.getId().isBlank())
            ? request.getId()
            : java.util.UUID.randomUUID().toString().substring(0, 8);
        entity.setId(id);
        entity.markNew();
        entity.setName(request.getName());
        entity.setTargetServiceId(request.getTargetServiceId());
        entity.setTargetPort(request.getTargetPort());
        entity.setMethod(request.getMethod());
        entity.setApiPath(request.getApiPath());
        entity.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        entity.setDescription(request.getDescription());
        entity.setRouteType(request.getRouteType() != null ? request.getRouteType() : "MANUAL");
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        entity.setStripPrefix(request.getStripPrefix());
        entity.setTimeoutMs(request.getTimeoutMs());
        entity.setRetryCount(request.getRetryCount());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return routeRepository.save(entity)
            .flatMap(saved -> saveConditions(saved.getId(), request.getConditions())
                .then(conditionRepository.findByRouteId(saved.getId()).collectList())
                .flatMap(conditions -> {
                    Mono<Void> routeAction = Boolean.FALSE.equals(saved.getEnabled())
                        ? Mono.empty()
                        : routeDefinitionWriter.save(Mono.just(buildDefinition(saved, conditions)));
                    return routeAction
                        .then(Mono.fromRunnable(() -> {
                            manualRouteIds.add(saved.getId());
                            publisher.publishEvent(new RefreshRoutesEvent(this));
                            log.info("[Route] 추가: id={}, path={}, enabled={}", saved.getId(), saved.getApiPath(), saved.getEnabled());
                        }))
                        .thenReturn(saved);
                })
            );
    }

    public Mono<RouteEntity> updateRoute(String id, RouteRequest request) {
        return routeRepository.findById(id)
            .flatMap(entity -> {
                entity.setName(request.getName());
                entity.setTargetServiceId(request.getTargetServiceId());
                entity.setTargetPort(request.getTargetPort());
                entity.setMethod(request.getMethod());
                entity.setApiPath(request.getApiPath());
                entity.setPriority(request.getPriority() != null ? request.getPriority() : 0);
                entity.setDescription(request.getDescription());
                entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
                entity.setStripPrefix(request.getStripPrefix());
                entity.setTimeoutMs(request.getTimeoutMs());
                entity.setRetryCount(request.getRetryCount());
                entity.setUpdatedAt(LocalDateTime.now());
                return routeRepository.save(entity);
            })
            .flatMap(saved -> conditionRepository.deleteByRouteId(saved.getId())
                .then(saveConditions(saved.getId(), request.getConditions()))
                .then(conditionRepository.findByRouteId(saved.getId()).collectList())
                .flatMap(conditions -> {
                    Mono<Void> routeAction = Boolean.FALSE.equals(saved.getEnabled())
                        ? routeDefinitionWriter.delete(Mono.just(id)).onErrorResume(e -> Mono.empty())
                        : routeDefinitionWriter.delete(Mono.just(id)).onErrorResume(e -> Mono.empty())
                            .then(routeDefinitionWriter.save(Mono.just(buildDefinition(saved, conditions))));
                    return routeAction
                        .then(Mono.fromRunnable(() -> publisher.publishEvent(new RefreshRoutesEvent(this))))
                        .thenReturn(saved);
                })
            );
    }

    public Mono<Void> deleteRoute(String id) {
        return routeDefinitionWriter.delete(Mono.just(id)).onErrorResume(e -> Mono.empty())
            .then(conditionRepository.deleteByRouteId(id))
            .then(routeRepository.deleteById(id))
            .then(Mono.fromRunnable(() -> {
                manualRouteIds.remove(id);
                publisher.publishEvent(new RefreshRoutesEvent(this));
                log.info("[Route] 삭제: id={}", id);
            }));
    }

    public Flux<RouteEntity> findAllRoutes() {
        return routeRepository.findAllByOrderByCreatedAtAsc();
    }

    public Mono<List<RouteConditionEntity>> findConditions(String routeId) {
        return conditionRepository.findByRouteId(routeId).collectList();
    }

    public boolean isManual(String routeId) {
        return manualRouteIds.contains(routeId);
    }

    private Mono<Void> saveConditions(String routeId, List<RouteRequest.ConditionRequest> conditions) {
        if (conditions == null || conditions.isEmpty()) return Mono.empty();
        List<RouteConditionEntity> entities = new ArrayList<>();
        for (RouteRequest.ConditionRequest c : conditions) {
            RouteConditionEntity e = new RouteConditionEntity();
            e.setRouteId(routeId);
            e.setType(c.getType());
            e.setOperation(c.getOperation());
            e.setAttributeName(c.getAttributeName());
            e.setAttributeValue(c.getAttributeValue());
            entities.add(e);
        }
        return conditionRepository.saveAll(entities).then();
    }

    private RouteDefinition buildDefinition(RouteEntity entity, List<RouteConditionEntity> conditions) {
        RouteDefinition def = new RouteDefinition();
        def.setId(entity.getId());
        def.setUri(URI.create("lb://" + entity.getTargetServiceId()));

        List<PredicateDefinition> predicates = new ArrayList<>();

        // Path predicate
        predicates.add(new PredicateDefinition("Path=" + entity.getApiPath()));

        // Method predicate
        if (entity.getMethod() != null && !entity.getMethod().isBlank() && !"ALL".equals(entity.getMethod())) {
            predicates.add(new PredicateDefinition("Method=" + entity.getMethod()));
        }

        // 상세 조건 predicates
        if (conditions != null) {
            for (RouteConditionEntity c : conditions) {
                if ("Header".equals(c.getType())) {
                    predicates.add(new PredicateDefinition("Header=" + c.getAttributeName() + "," + c.getAttributeValue()));
                } else if ("Query".equals(c.getType())) {
                    predicates.add(new PredicateDefinition("Query=" + c.getAttributeName() + "," + c.getAttributeValue()));
                }
            }
        }

        def.setPredicates(predicates);

        List<FilterDefinition> filters = new ArrayList<>();
        int stripCount;
        if (entity.getStripPrefix() != null) {
            stripCount = entity.getStripPrefix();
        } else {
            stripCount = (int) java.util.Arrays.stream(entity.getApiPath().split("/"))
                    .filter(s -> !s.isEmpty() && !s.contains("*"))
                    .count();
            stripCount = Math.max(1, stripCount);
        }
        filters.add(new FilterDefinition("StripPrefix=" + stripCount));

        // 재시도 필터 (retryCount > 0 인 경우에만 추가)
        if (entity.getRetryCount() != null && entity.getRetryCount() > 0) {
            FilterDefinition retry = new FilterDefinition();
            retry.setName("Retry");
            retry.addArg("retries",                    String.valueOf(entity.getRetryCount()));
            retry.addArg("statuses",                   "BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT");
            retry.addArg("methods",                    "GET,HEAD,OPTIONS");
            retry.addArg("backoff.firstBackoff",       "50ms");
            retry.addArg("backoff.maxBackoff",         "500ms");
            retry.addArg("backoff.factor",             "2");
            retry.addArg("backoff.basedOnPreviousValue", "false");
            filters.add(retry);
        }

        def.setFilters(filters);

        // 타임아웃 설정 (null 이면 전역값 사용)
        if (entity.getTimeoutMs() != null) {
            def.getMetadata().put("response-timeout", entity.getTimeoutMs().longValue());
        }

        def.setOrder(entity.getPriority() != null ? entity.getPriority() : 0);
        return def;
    }
}
