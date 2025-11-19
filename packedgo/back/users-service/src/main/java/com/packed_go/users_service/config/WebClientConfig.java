package com.packed_go.users_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.services.event-service.base-url:http://localhost:8083}")
    private String eventServiceBaseUrl;

    @Bean(name = "eventServiceWebClient")
    public WebClient eventServiceWebClient() {
        return WebClient.builder()
                .baseUrl(eventServiceBaseUrl)
                .build();
    }
}
