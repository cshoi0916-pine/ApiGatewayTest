package com.example.apigateway.init;

import com.example.apigateway.apikey.ApiClient;
import com.example.apigateway.apikey.ApiClientRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final ApiClientRepository repository;

    public DataInitializer(ApiClientRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {

        if (repository.count() == 0) {

            repository.save(
                    new ApiClient(
                            "test-key-1",
                            "client-A",
                            5
                    )
            );

            repository.save(
                    new ApiClient(
                            "premium-key",
                            "premium-client",
                            100
                    )
            );
        }
    }
}