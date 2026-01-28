package com.medibridge.appointment_service_medibridge.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * REST Template Configuration
 * Configures RestTemplate for calling other microservices
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate bean for service-to-service communication
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(5000))
                .readTimeout(Duration.ofMillis(10000))
                .build();
    }
}
