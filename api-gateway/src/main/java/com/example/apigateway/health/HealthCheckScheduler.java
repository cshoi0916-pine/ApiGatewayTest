package com.example.apigateway.health;

import com.example.apigateway.registry.BackendServer;
import com.example.apigateway.registry.ServerRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

//@Component
public class HealthCheckScheduler {

    private final ServerRegistry registry;

    public HealthCheckScheduler(ServerRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHealth() {

        for (BackendServer server : registry.getServers()) {

            boolean alive = isServerAlive(server);

            server.setAlive(alive);

            System.out.println(
                    server.getPort() +
                            " health = " +
                            alive
            );
        }
    }

    private boolean isServerAlive(BackendServer server) {

        try {

            URL url = new URL(
                    "http://" +
                            server.getHost() +
                            ":" +
                            server.getPort() +
                            "/hello"
            );

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);

            int responseCode = connection.getResponseCode();

            return responseCode == 200;

        } catch (Exception e) {

            return false;
        }
    }
}