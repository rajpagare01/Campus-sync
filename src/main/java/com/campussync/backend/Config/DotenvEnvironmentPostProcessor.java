package com.campussync.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "campusSyncDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenvPath = Paths.get(".env").toAbsolutePath().normalize();
        
        if (!Files.exists(dotenvPath)) {
            // Try to look in parent directory if not found (useful for some IDE setups)
            dotenvPath = Paths.get("..", ".env").toAbsolutePath().normalize();
            if (!Files.exists(dotenvPath)) {
                System.out.println("[Dotenv] .env file not found at " + Paths.get(".env").toAbsolutePath());
                return;
            }
        }

        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        System.out.println("[Dotenv] Loading environment from " + dotenvPath);
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(dotenvPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("[Dotenv] Failed to load .env: " + e.getMessage());
            return;
        }

        Map<String, Object> dotenvValues = new LinkedHashMap<>();
        for (String propertyName : properties.stringPropertyNames()) {
            String value = properties.getProperty(propertyName);
            if (value != null) {
                dotenvValues.put(propertyName, value.trim());
            }
        }

        // Add to the front of property sources so it overrides defaults but can be overridden by system properties
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvValues));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
