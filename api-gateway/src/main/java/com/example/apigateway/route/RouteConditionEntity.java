package com.example.apigateway.route;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("gateway.route_condition")
public class RouteConditionEntity {

    @Id
    private Long id;
    private String routeId;
    private String type;
    private String operation;
    private String attributeName;
    private String attributeValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getAttributeName() { return attributeName; }
    public void setAttributeName(String attributeName) { this.attributeName = attributeName; }

    public String getAttributeValue() { return attributeValue; }
    public void setAttributeValue(String attributeValue) { this.attributeValue = attributeValue; }
}
