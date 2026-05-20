package com.campussync.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RuntimeProfileValidator {

    public RuntimeProfileValidator(Environment environment,
                                   AppRuntimeProperties runtimeProperties,
                                   @Value("${spring.data.redis.host:}") String redisHost) {
        boolean prod = environment.matchesProfiles("prod");
       /* if (prod && (!runtimeProperties.requireRedis() || !StringUtils.hasText(redisHost))) {
            throw new IllegalStateException("Production profile requires Redis-backed token stores. Set SECURITY_TOKENS_REQUIRE_REDIS=true and Redis connection properties.");
        }*/
    }
}
