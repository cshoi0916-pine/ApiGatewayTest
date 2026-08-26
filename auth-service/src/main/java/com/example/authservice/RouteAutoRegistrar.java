package com.example.authservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class RouteAutoRegistrar {

    private static final Logger log = LoggerFactory.getLogger(RouteAutoRegistrar.class);

    private final RestTemplate restTemplate;

    @Value("${pine-gw.url}")
    private String gatewayUrl;

    public RouteAutoRegistrar(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerRoute() {
        try {
            String routesUrl = gatewayUrl + "/console/api/routes";

            List<Map<String, Object>> routes = restTemplate.exchange(
                routesUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();

            boolean exists = routes != null && routes.stream()
                .anyMatch(r -> "/auth/**".equals(r.get("apiPath"))
                            && "auth-service".equals(r.get("targetServiceId")));

            if (exists) {
                log.info("[Route] auth 라우트 이미 등록됨 — 건너뜀");
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of(
                "id",              "auth-service-route",
                "name",            "Auth Service",
                "targetServiceId", "auth-service",
                "apiPath",         "/auth/**",
                "method",          "ALL",
                "priority",        10,
                "enabled",         true,
                "routeType",       "AUTO"
            ), headers);

            restTemplate.postForEntity(routesUrl, entity, Map.class);
            log.info("[Route] auth 라우트 자동 등록 완료: /auth/**");

        } catch (Exception e) {
            log.warn("[Route] auth 라우트 자동 등록 실패: {}", e.getMessage());
        }
    }
}
