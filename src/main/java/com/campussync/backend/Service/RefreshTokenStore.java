package com.campussync.backend.Service;

import java.util.Optional;

public interface RefreshTokenStore {

    void store(String email, String refreshToken);

    Optional<String> findOwner(String refreshToken);

    void revoke(String email, String refreshToken);

    void revokeAll(String email);
}
