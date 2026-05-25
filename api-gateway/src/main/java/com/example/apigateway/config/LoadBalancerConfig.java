package com.example.apigateway.config;

import com.example.apigateway.registry.BackendServer;
import com.example.apigateway.registry.ServerRegistry;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
public class LoadBalancerConfig {

    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(
            ServerRegistry registry
    ) {

        return new ServiceInstanceListSupplier() {

            @Override
            public String getServiceId() {
                return "backend-service";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {

                List<ServiceInstance> instances =
                        registry.getServers()
                                .stream()
                                .filter(BackendServer::isAlive)

                                .map(server -> (ServiceInstance)
                                        new DefaultServiceInstance(
                                                server.getHost() + ":" + server.getPort(),
                                                "backend-service",
                                                server.getHost(),
                                                server.getPort(),
                                                false
                                        )
                                )
                                .toList();

                return Flux.just(instances);
            }
        };
    }
}