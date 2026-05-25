package com.example.apigateway.admin;

import com.example.apigateway.apikey.ApiClient;
import com.example.apigateway.apikey.ApiClientRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ApiKeyAdminService {

    private final ApiClientRepository repository;

    public ApiKeyAdminService(ApiClientRepository repository) {
        this.repository = repository;
    }

    public String createApiKey(
            String name,
            int limitPerMinute
    ) {

        String apiKey =
                "sk_live_" +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "");

        ApiClient client = new ApiClient(
                apiKey,
                name,
                limitPerMinute
        );

        repository.save(client);

        return apiKey;
    }
}