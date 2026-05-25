package com.example.apigateway.health;

import com.example.apigateway.registry.BackendServer;
import com.example.apigateway.registry.ServerRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BackendHealthChecker {

    private final ServerRegistry registry;

    private final WebClient webClient =
            WebClient.builder()
                    .build();

    private final ConcurrentHashMap<String, Boolean> healthStates =
            new ConcurrentHashMap<>();

    public BackendHealthChecker(ServerRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHealth() {

        for (BackendServer server : registry.getKnownServers()) {

            String key =
                    server.getHost() +
                            ":" +
                            server.getPort();

            String url =
                    "http://" +
                            key +
                            "/actuator/health";

            try {

                webClient.get()
                        .uri(url)
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(2))
                        .block();

                Boolean previous =
                        healthStates.getOrDefault(key, false);

                if (!previous) {

                    System.out.println(
                            "[HEALTH] " +
                                    key +
                                    " SERVER RECOVERED"
                    );
                }

                healthStates.put(key, true);

                registry.activateServer(server);

            } catch (Exception e) {

                Boolean previous =
                        healthStates.getOrDefault(key, true);

                if (previous) {

                    System.out.println(
                            "[HEALTH] " +
                                    key +
                                    " SERVER DOWN"
                    );

                    System.out.println(
                            "[ERROR] " +
                                    e.getClass().getSimpleName() +
                                    " : " +
                                    e.getMessage()
                    );
                }

                healthStates.put(key, false);

                registry.deactivateServer(
                        server.getHost(),
                        server.getPort()
                );
            }
        }
    }
}