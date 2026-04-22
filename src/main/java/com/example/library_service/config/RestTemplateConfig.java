package com.example.library_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
// Configuration class to define RestTemplate bean for inter-service communication
public class RestTemplateConfig {

    @Bean
    // Provides RestTemplate instance used to call external microservices (e.g., Finance)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}