package com.example.apigateway.loadbalancer;

import com.example.apigateway.registry.ServiceRegistryService;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.List;

public class CustomServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    private final ServiceRegistryService registryService;

    public CustomServiceInstanceListSupplier(String serviceId, ServiceRegistryService registryService) {
        this.serviceId = serviceId;
        this.registryService = registryService;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return registryService.getInstances(serviceId)
            .map(e -> (ServiceInstance) new DefaultServiceInstance(
                e.getId().toString(),
                e.getServiceId(),
                e.getHost(),
                e.getPort(),
                "HTTPS".equalsIgnoreCase(e.getProtocol())
            ))
            .collectList()
            .flux();
    }
}
