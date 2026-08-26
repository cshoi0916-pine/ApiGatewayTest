package com.example.apigateway.exception;

public class GatewayServiceException extends RuntimeException {

    public enum Reason {
        NOT_FOUND, SERVICE_DISABLED, NO_INSTANCES, ALL_DOWN
    }

    private final Reason reason;
    private final String serviceId;

    public GatewayServiceException(Reason reason, String serviceId) {
        super(reason.name() + ": " + serviceId);
        this.reason = reason;
        this.serviceId = serviceId;
    }

    public Reason getReason() { return reason; }
    public String getServiceId() { return serviceId; }
}
