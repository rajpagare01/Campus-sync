package com.campussync.backend.Service;

import java.time.Duration;
import java.util.Optional;

public interface TransientStateStore {

    void put(String key, String value, Duration ttl);

    Optional<String> get(String key);

    boolean exists(String key);

    void delete(String... keys);
}
