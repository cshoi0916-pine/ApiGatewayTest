package com.example.apigateway.loadbalancer;

import com.example.apigateway.registry.ServiceRegistryService;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@LoadBalancerClients(defaultConfiguration = CustomLoadBalancerConfig.LoadBalancerBeanConfig.class)
public class CustomLoadBalancerConfig {

    public static class LoadBalancerBeanConfig {

        @Bean
        public ServiceInstanceListSupplier serviceInstanceListSupplier(
                Environment env,
                ServiceRegistryService registryService) {
            String serviceId = env.getProperty("loadbalancer.client.name", "");
            return new CustomServiceInstanceListSupplier(serviceId, registryService);
        }
    }
}
