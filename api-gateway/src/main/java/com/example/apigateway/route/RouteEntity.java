package com.example.apigateway.route;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("gateway.gateway_route")
public class RouteEntity implements Persistable<String> {

    @Id
    private String id;

    @Transient
    private boolean newEntity;
    private String name;
    private String targetServiceId;
    private Integer targetPort;
    private String method;
    private String apiPath;
    private Integer priority;
    private String description;
    private String routeType;
    private Boolean enabled;
    private Integer stripPrefix;
    private Integer timeoutMs;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public boolean isNew() { return newEntity; }
    public void markNew() { this.newEntity = true; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetServiceId() { return targetServiceId; }
    public void setTargetServiceId(String targetServiceId) { this.targetServiceId = targetServiceId; }

    public Integer getTargetPort() { return targetPort; }
    public void setTargetPort(Integer targetPort) { this.targetPort = targetPort; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRouteType() { return routeType; }
    public void setRouteType(String routeType) { this.routeType = routeType; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Integer getStripPrefix() { return stripPrefix; }
    public void setStripPrefix(Integer stripPrefix) { this.stripPrefix = stripPrefix; }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
