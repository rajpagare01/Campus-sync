package com.campussync.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String provider,
        String publicBaseUrl,
        Duration signedUrlTtl,
        S3 s3
) {
    public record S3(
            String bucket,
            String region,
            String endpoint,
            String accessKey,
            String secretKey,
            boolean pathStyleAccess
    ) {
    }
}
