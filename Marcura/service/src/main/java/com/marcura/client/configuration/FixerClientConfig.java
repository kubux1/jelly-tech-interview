package com.marcura.client.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.client.FixerClient;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FixerClientConfig {
    private final ObjectMapper objectMapper;

    @Value("${api.fixer.url}")
    private String serviceUrl;

    @Bean
    public FixerClient fixerClient() {
        return Feign.builder()
                    .client(new OkHttpClient())
                    .encoder(new JacksonEncoder(objectMapper))
                    .decoder(new JacksonDecoder(objectMapper))
                    .logger(new Slf4jLogger(FixerClient.class))
                    .logLevel(Logger.Level.BASIC)
                    .target(FixerClient.class, serviceUrl);
    }
}
