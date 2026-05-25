package com.example.backendserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello(
            @RequestHeader(value = "X-Gateway", required = false)
            String gatewayHeader,

            @RequestHeader(value = "X-TRACE-ID", required = false)
            String traceId
    ) {

        return "hello backend-01"
                + "\ngateway=" + gatewayHeader
                + "\ntraceId=" + traceId;
    }
}