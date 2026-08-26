package com.example.apigateway.registry;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 서비스 자동등록 API — 각 서비스의 ApplicationReadyEvent에서 호출
 */
@RestController
@RequestMapping("/registry")
public class ServiceRegistryController {

    private final ServiceRegistryService registryService;

    public ServiceRegistryController(ServiceRegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/register")
    public Mono<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        String serviceId = (String) body.get("serviceId");
        String host     = (String) body.get("host");
        Integer port    = (Integer) body.get("port");
        String protocol = (String) body.getOrDefault("protocol", "HTTP");

        return registryService.register(serviceId, host, port, protocol)
            .map(e -> Map.of("result", "registered", "serviceId", serviceId));
    }

    @DeleteMapping("/deregister")
    public Mono<Map<String, Object>> deregister(@RequestBody Map<String, Object> body) {
        String serviceId = (String) body.get("serviceId");
        String host     = (String) body.get("host");
        Integer port    = (Integer) body.get("port");

        return registryService.deregister(serviceId, host, port)
            .thenReturn(Map.of("result", "deregistered", "serviceId", serviceId));
    }

    @PutMapping("/heartbeat")
    public Mono<Map<String, Object>> heartbeat(@RequestBody Map<String, Object> body) {
        String serviceId = (String) body.get("serviceId");
        String host     = (String) body.get("host");
        Integer port    = (Integer) body.get("port");

        return registryService.heartbeat(serviceId, host, port)
            .thenReturn(Map.of("result", "ok"));
    }
}
