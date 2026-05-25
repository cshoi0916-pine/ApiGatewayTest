package com.example.apigateway.apikey;

import jakarta.persistence.*;

@Entity
@Table(name = "api_client")
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String apiKey;

    private String name;

    private int limitPerMinute;

    public ApiClient() {
    }

    public ApiClient(
            String apiKey,
            String name,
            int limitPerMinute
    ) {
        this.apiKey = apiKey;
        this.name = name;
        this.limitPerMinute = limitPerMinute;
    }

    public Long getId() {
        return id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getName() {
        return name;
    }

    public int getLimitPerMinute() {
        return limitPerMinute;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLimitPerMinute(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }
}