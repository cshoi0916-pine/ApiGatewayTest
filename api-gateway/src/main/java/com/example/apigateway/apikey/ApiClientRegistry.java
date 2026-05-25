package com.example.apigateway.apikey;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Component
public class ApiClientRegistry {

    private final Map<String, ApiClient> clients =
            new ConcurrentHashMap<>();

    public ApiClientRegistry() {

        clients.put(
                "test-key-1",
                new ApiClient(
                        "test-key-1",
                        "client-A",
                        5
                )
        );

        clients.put(
                "premium-key",
                new ApiClient(
                        "premium-key",
                        "premium-client",
                        100
                )
        );
    }

    public ApiClient getClient(String apiKey) {
        return clients.get(apiKey);
    }
}