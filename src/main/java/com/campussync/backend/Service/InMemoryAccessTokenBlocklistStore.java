package com.campussync.backend.Service;

import com.campussync.backend.config.AppRuntimeProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile({"dev", "test", "default"})
public class InMemoryAccessTokenBlocklistStore implements AccessTokenBlocklistStore {

    private final AppRuntimeProperties runtimeProperties;
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public InMemoryAccessTokenBlocklistStore(AppRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public void blacklist(String token, Instant expiresAt) {
        assertFallbackAllowed();
        blacklist.put(token, expiresAt);
    }

    @Override
    public boolean isBlacklisted(String token) {
        assertFallbackAllowed();
        cleanup();
        Instant expiresAt = blacklist.get(token);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    private void cleanup() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private void assertFallbackAllowed() {
        if (runtimeProperties.requireRedis() || !runtimeProperties.allowInMemoryFallback()) {
            throw new IllegalStateException("In-memory access token blacklist is disabled outside local development");
        }
    }
}
