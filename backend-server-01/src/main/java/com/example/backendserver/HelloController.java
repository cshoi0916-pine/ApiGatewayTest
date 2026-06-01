package com.example.backendserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello(
            @RequestHeader(value = "X-Gateway", required = false) String gatewayHeader,
            @RequestHeader(value = "X-TRACE-ID", required = false) String traceId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        return "hello from backend-01"
                + "\ngateway=" + gatewayHeader
                + "\ntraceId=" + traceId
                + "\nuserId=" + userId
                + "\nuserRole=" + userRole;
    }

    @GetMapping("/admin/test")
    public String admin(
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return "ADMIN API SUCCESS (backend-01), requestedBy=" + userId;
    }
}