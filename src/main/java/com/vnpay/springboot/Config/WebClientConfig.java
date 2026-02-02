package com.vnpay.springboot.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient vnpayWebClient(VNPayConfig vnPayConfig, WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(vnPayConfig.getApiUrl()).build();
    }
}
