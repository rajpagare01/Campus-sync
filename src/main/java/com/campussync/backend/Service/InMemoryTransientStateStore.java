package com.campussync.backend.Service;

import com.campussync.backend.config.AppRuntimeProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile({"dev", "test", "default"})
public class InMemoryTransientStateStore implements TransientStateStore {

    private final AppRuntimeProperties runtimeProperties;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public InMemoryTransientStateStore(AppRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        assertFallbackAllowed();
        entries.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> get(String key) {
        assertFallbackAllowed();
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public boolean exists(String key) {
        return get(key).isPresent();
    }

    @Override
    public void delete(String... keys) {
        assertFallbackAllowed();
        Arrays.stream(keys).forEach(entries::remove);
    }

    private void assertFallbackAllowed() {
        if (runtimeProperties.requireRedis() || !runtimeProperties.allowInMemoryFallback()) {
            throw new IllegalStateException("In-memory transient state storage is disabled outside local development");
        }
    }

    private record Entry(String value, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
