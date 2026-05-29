package com.campussync.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Profile("prod")
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> redisValueSerializer
    ) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)) // Default TTL: 15 minutes
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("userProfiles",
                        config
                                .entryTtl(Duration.ofMinutes(15))) // User profiles: 15 min
                .withCacheConfiguration("followStats",
                        config
                                .entryTtl(Duration.ofMinutes(10))) // Follow stats: 10 min
                .withCacheConfiguration("feedStats",
                        config
                                .entryTtl(Duration.ofMinutes(5))) // Feed stats: 5 min
                .withCacheConfiguration("postLikeCounts",
                        config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("userPostLikes",
                        config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("postCommentCounts",
                        config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("eventsCache",
                        config.entryTtl(Duration.ofMinutes(15)))
                .build();
    }
}
