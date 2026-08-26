package com.example.apigateway.apikey;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("gateway.api_client")
public class ApiClient {

    @Id
    private Long id;
    private String clientName;
    private String apiKey;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private boolean enabled;

    public Long getId() { return id; }
    public String getClientName() { return clientName; }
    public String getApiKey() { return apiKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public boolean isEnabled() { return enabled; }

    public void setId(Long id) { this.id = id; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
