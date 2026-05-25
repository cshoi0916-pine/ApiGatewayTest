package com.example.apigateway.admin.dto;

public class CreateApiKeyRequest {

    private String name;

    private int limitPerMinute;

    public String getName() {
        return name;
    }

    public int getLimitPerMinute() {
        return limitPerMinute;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLimitPerMinute(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }
}