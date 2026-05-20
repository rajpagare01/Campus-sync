package com.campussync.backend.Service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Profile("prod")
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final long REFRESH_TOKEN_TTL_DAYS = 7;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRefreshTokenStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void store(String email, String refreshToken) {
        redisTemplate.opsForValue().set(refreshTokenKey(refreshToken), email, REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForSet().add(userRefreshKey(email), refreshToken);
        redisTemplate.expire(userRefreshKey(email), REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public Optional<String> findOwner(String refreshToken) {
        Object value = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));
        return value instanceof String email && !email.isBlank() ? Optional.of(email) : Optional.empty();
    }

    @Override
    public void revoke(String email, String refreshToken) {
        redisTemplate.delete(refreshTokenKey(refreshToken));
        if (email != null && !email.isBlank()) {
            redisTemplate.opsForSet().remove(userRefreshKey(email), refreshToken);
        }
    }

    @Override
    public void revokeAll(String email) {
        SetOperations<String, Object> setOperations = redisTemplate.opsForSet();
        Set<Object> tokens = setOperations.members(userRefreshKey(email));
        if (tokens != null && !tokens.isEmpty()) {
            List<String> keysToDelete = new ArrayList<>();
            for (Object token : tokens) {
                if (token != null) {
                    keysToDelete.add(refreshTokenKey(String.valueOf(token)));
                }
            }
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        }
        redisTemplate.delete(userRefreshKey(email));
    }

    private String refreshTokenKey(String refreshToken) {
        return "REFRESH_" + refreshToken;
    }

    private String userRefreshKey(String email) {
        return "USER_REFRESH_" + email;
    }
}
