package com.campussync.backend.Service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Profile("prod")
public class RedisAccessTokenBlocklistStore implements AccessTokenBlocklistStore {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisAccessTokenBlocklistStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String token, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(blacklistKey(token), "BLACKLISTED", ttl);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(token)));
    }

    private String blacklistKey(String token) {
        return "JWT_BLACKLIST_" + token;
    }
}
