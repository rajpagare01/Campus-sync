package com.campussync.backend.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProductionSecretsValidator {

    private static final String PLACEHOLDER_SECRET = "replace-this-with-a-strong-32-plus-char-secret";

    private final Environment environment;

    public ProductionSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        Map<String, String> required = new LinkedHashMap<>();
        required.put("spring.datasource.username", environment.getProperty("spring.datasource.username"));
        required.put("spring.datasource.password", environment.getProperty("spring.datasource.password"));
        required.put("jwt.secret", environment.getProperty("jwt.secret"));
        required.put("payment.razorpay.key-id", environment.getProperty("payment.razorpay.key-id"));
        required.put("payment.razorpay.key-secret", environment.getProperty("payment.razorpay.key-secret"));
        required.put("payment.razorpay.webhook-secret", environment.getProperty("payment.razorpay.webhook-secret"));
        required.put("spring.data.redis.host", environment.getProperty("spring.data.redis.host"));
        required.put("spring.data.redis.password", environment.getProperty("spring.data.redis.password"));

        required.forEach((key, value) -> {
            if (value == null || value.isBlank() || PLACEHOLDER_SECRET.equals(value)) {
                throw new IllegalStateException("Missing required production secret/config: " + key);
            }
        });
    }
}
