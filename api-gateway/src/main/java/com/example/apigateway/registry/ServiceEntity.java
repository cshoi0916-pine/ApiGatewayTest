package com.example.apigateway.registry;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("gateway.service")
public class ServiceEntity implements Persistable<String> {

    @Id
    private String serviceId;

    @Transient
    private boolean newEntity;

    private String serviceName;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;

    @Override
    public String getId() { return serviceId; }

    @Override
    public boolean isNew() { return newEntity; }
    public void markNew() { this.newEntity = true; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
