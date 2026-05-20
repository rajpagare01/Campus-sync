package com.campussync.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.tokens")
public record AppRuntimeProperties(
        boolean requireRedis,
        boolean allowInMemoryFallback
) {
}
