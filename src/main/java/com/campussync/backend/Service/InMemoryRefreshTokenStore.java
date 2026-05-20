package com.campussync.backend.Service;

import com.campussync.backend.config.AppRuntimeProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile({"dev", "test", "default"})
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final AppRuntimeProperties runtimeProperties;
    private final ConcurrentHashMap<String, String> ownersByToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> tokensByUser = new ConcurrentHashMap<>();

    public InMemoryRefreshTokenStore(AppRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public void store(String email, String refreshToken) {
        assertFallbackAllowed();
        ownersByToken.put(refreshToken, email);
        tokensByUser.computeIfAbsent(email, ignored -> ConcurrentHashMap.newKeySet()).add(refreshToken);
    }

    @Override
    public Optional<String> findOwner(String refreshToken) {
        assertFallbackAllowed();
        return Optional.ofNullable(ownersByToken.get(refreshToken));
    }

    @Override
    public void revoke(String email, String refreshToken) {
        assertFallbackAllowed();
        ownersByToken.remove(refreshToken);
        if (email != null) {
            Set<String> tokens = tokensByUser.get(email);
            if (tokens != null) {
                tokens.remove(refreshToken);
                if (tokens.isEmpty()) {
                    tokensByUser.remove(email);
                }
            }
        }
    }

    @Override
    public void revokeAll(String email) {
        assertFallbackAllowed();
        Set<String> tokens = tokensByUser.remove(email);
        if (tokens != null) {
            tokens.forEach(ownersByToken::remove);
        }
    }

    private void assertFallbackAllowed() {
        if (runtimeProperties.requireRedis() || !runtimeProperties.allowInMemoryFallback()) {
            throw new IllegalStateException("In-memory refresh token storage is disabled outside local development");
        }
    }
}
