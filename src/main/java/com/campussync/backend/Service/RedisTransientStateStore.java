package com.campussync.backend.Service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
public class RedisTransientStateStore implements TransientStateStore {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTransientStateStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void delete(String... keys) {
        redisTemplate.delete(Arrays.asList(keys));
    }
}
