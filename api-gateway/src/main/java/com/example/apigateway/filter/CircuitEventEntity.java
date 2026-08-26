package com.example.apigateway.filter;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("gateway.circuit_event")
public class CircuitEventEntity {

    @Id
    private Long id;
    private String serviceId;
    private String event;
    private String detail;
    private LocalDateTime eventAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
}
