package com.example.apigateway.console.dto;

import java.util.List;

public class RouteRequest {
    private String id;
    private String name;
    private String targetServiceId;
    private Integer targetPort;
    private String method;
    private String apiPath;
    private Integer priority;
    private String description;
    private Boolean enabled;
    private String routeType;
    private Integer stripPrefix;
    private Integer timeoutMs;
    private Integer retryCount;
    private List<ConditionRequest> conditions;

    public static class ConditionRequest {
        private String type;
        private String operation;
        private String attributeName;
        private String attributeValue;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getAttributeName() { return attributeName; }
        public void setAttributeName(String attributeName) { this.attributeName = attributeName; }
        public String getAttributeValue() { return attributeValue; }
        public void setAttributeValue(String attributeValue) { this.attributeValue = attributeValue; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getRouteType() { return routeType; }
    public void setRouteType(String routeType) { this.routeType = routeType; }
    public Integer getStripPrefix() { return stripPrefix; }
    public void setStripPrefix(Integer stripPrefix) { this.stripPrefix = stripPrefix; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public List<ConditionRequest> getConditions() { return conditions; }
    public void setConditions(List<ConditionRequest> conditions) { this.conditions = conditions; }

    // DynamicRouteService 호환용
    public String getPath() { return apiPath; }
    public String getUri() {
        if (targetServiceId != null && !targetServiceId.isBlank()) return "lb://" + targetServiceId;
        return null;
    }
}
