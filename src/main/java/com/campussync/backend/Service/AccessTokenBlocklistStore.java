package com.campussync.backend.Service;

import java.time.Instant;

public interface AccessTokenBlocklistStore {

    void blacklist(String token, Instant expiresAt);

    boolean isBlacklisted(String token);
}
