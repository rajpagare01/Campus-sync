package com.campussync.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jobs")
public record JobRuntimeProperties(
        String mode
) {
    public boolean useRabbit() {
        return mode != null && mode.equalsIgnoreCase("rabbit");
    }
}
