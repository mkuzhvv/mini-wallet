package com.mini_wallet.payment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LedgerClientConfig {

    @Bean
    RestClient ledgerClient(@Value("${ledger.base-url}") String baseUrl,
                            @Value("${ledger.connect-timeout:2s}") Duration connectTimeout,
                            @Value("${ledger.read-timeout:3s}") Duration readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
