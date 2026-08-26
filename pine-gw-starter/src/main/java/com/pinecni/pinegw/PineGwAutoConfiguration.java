package com.pinecni.pinegw;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(PineGwProperties.class)
@ConditionalOnProperty(prefix = "pine-gw", name = "url")
public class PineGwAutoConfiguration {

    @Bean
    public PineGwRegistrar pineGwRegistrar(PineGwProperties props, Environment env) {
        return new PineGwRegistrar(props, env);
    }
}
