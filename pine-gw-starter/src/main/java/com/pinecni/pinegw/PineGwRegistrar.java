package com.pinecni.pinegw;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Map;

public class PineGwRegistrar {

    private static final Logger log = LoggerFactory.getLogger(PineGwRegistrar.class);

    private final String gatewayUrl;
    private final String serviceId;
    private final String host;
    private final int port;
    private final PineGwProperties.RouteConfig routeConfig;

    private final RestClient restClient;
    private volatile boolean registered = false;

    public PineGwRegistrar(PineGwProperties props, Environment env) {
        this.gatewayUrl  = props.getUrl();
        this.restClient  = buildRestClient(props.getUrl());
        this.serviceId   = props.getServiceId();
        this.host        = props.getHost();
        this.port        = props.getPort() != null
            ? props.getPort()
            : env.getProperty("server.port", Integer.class, 8080);
        this.routeConfig = props.getRoute();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        tryRegister();
    }

    @Scheduled(fixedDelayString = "${pine-gw.heartbeat-interval-ms:20000}",
               initialDelayString = "${pine-gw.heartbeat-interval-ms:20000}")
    public void heartbeat() {
        if (!registered) {
            tryRegister();
            return;
        }
        try {
            restClient.put()
                .uri(gatewayUrl + "/registry/heartbeat")
                .body(Map.of("serviceId", serviceId, "host", host, "port", port))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[pine-gw] Heartbeat 실패, 재등록 시도: {}", e.getMessage());
            registered = false;
        }
    }

    private void tryRegister() {
        try {
            restClient.post()
                .uri(gatewayUrl + "/registry/register")
                .body(Map.of("serviceId", serviceId, "host", host, "port", port))
                .retrieve()
                .toBodilessEntity();
            registered = true;
            log.info("[pine-gw] 등록 완료: {}@{}:{}", serviceId, host, port);
        } catch (Exception e) {
            log.warn("[pine-gw] 등록 실패, {}초 후 재시도: {}", 20, e.getMessage());
            return;
        }

        if (routeConfig != null && routeConfig.getPath() != null) {
            tryRegisterRoute();
        }
    }

    private void tryRegisterRoute() {
        String routeId = serviceId + "-route";
        try {
            // 기존 라우팅 룰 삭제 (없으면 무시) → 재등록으로 멱등성 보장
            try {
                restClient.delete()
                    .uri(gatewayUrl + "/console/api/routes/" + routeId)
                    .retrieve()
                    .toBodilessEntity();
            } catch (Exception ignored) {}

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("id",              routeId);
            body.put("name",            serviceId + " 자동 라우팅");
            body.put("targetServiceId", serviceId);
            body.put("apiPath",         routeConfig.getPath());
            body.put("method",          routeConfig.getMethod() != null ? routeConfig.getMethod() : "ALL");
            body.put("priority",        routeConfig.getPriority());
            body.put("enabled",         routeConfig.isEnabled());
            body.put("routeType",       "AUTO");

            restClient.post()
                .uri(gatewayUrl + "/console/api/routes")
                .body(body)
                .retrieve()
                .toBodilessEntity();

            log.info("[pine-gw] 라우팅 룰 등록 완료: {} → {}", routeConfig.getPath(), serviceId);
        } catch (Exception e) {
            log.warn("[pine-gw] 라우팅 룰 등록 실패: {}", e.getMessage());
        }
    }

    private static RestClient buildRestClient(String url) {
        if (!url.startsWith("https")) {
            return RestClient.create();
        }
        // reactor-netty가 있으면 Netty SSL (WebFlux 앱) — 별도 클래스로 분리해 ClassLoader 오류 방지
        try {
            Class.forName("reactor.netty.http.client.HttpClient");
            return PineGwNettySslClientFactory.create();
        } catch (ClassNotFoundException ignored) {
            // reactor-netty 없는 환경 — JDK URLConnection 폴백
        } catch (Exception e) {
            log.warn("[pine-gw] Netty SSL 설정 실패: {}", e.getMessage());
        }
        // JDK URLConnection 전역 SSL 설정 (Spring MVC 앱)
        // X509ExtendedTrustManager: TLS 핸드셰이크 레벨 hostname 검증까지 우회 (X509TrustManager로는 불충분)
        try {
            TrustManager[] trustAll = { new X509ExtendedTrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
                public void checkClientTrusted(X509Certificate[] c, String a, Socket s) {}
                public void checkServerTrusted(X509Certificate[] c, String a, Socket s) {}
                public void checkClientTrusted(X509Certificate[] c, String a, SSLEngine e) {}
                public void checkServerTrusted(X509Certificate[] c, String a, SSLEngine e) {}
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, null);
            SSLContext.setDefault(sslContext);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            log.warn("[pine-gw] HTTPS SSL 설정 실패: {}", e.getMessage());
        }
        return RestClient.create();
    }

    @PreDestroy
    public void deregister() {
        try {
            restClient.method(HttpMethod.DELETE)
                .uri(gatewayUrl + "/registry/deregister")
                .body(Map.of("serviceId", serviceId, "host", host, "port", port))
                .retrieve()
                .toBodilessEntity();
            log.info("[pine-gw] 등록 해제: {}@{}:{}", serviceId, host, port);
        } catch (Exception e) {
            log.warn("[pine-gw] 등록 해제 실패: {}", e.getMessage());
        }
    }
}
