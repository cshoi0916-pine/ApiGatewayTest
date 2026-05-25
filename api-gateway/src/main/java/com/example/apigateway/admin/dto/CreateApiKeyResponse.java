package com.example.apigateway.admin.dto;

public class CreateApiKeyResponse {

    private String apiKey;

    public CreateApiKeyResponse(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }
}