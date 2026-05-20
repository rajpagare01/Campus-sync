package com.campussync.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppRuntimeProperties.class,
        StorageProperties.class,
        JobRuntimeProperties.class
})
public class PropertiesConfig {
}
