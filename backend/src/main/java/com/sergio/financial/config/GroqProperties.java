package com.sergio.financial.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "groq")
public record GroqProperties(
        String apiKey,
        URI baseUrl,
        Duration timeout,
        int transactionContextLimit) {
}
