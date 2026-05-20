package com.campussync.backend.config;

import com.campussync.backend.Service.AccessTokenBlocklistStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    private AccessTokenBlocklistStore accessTokenBlocklistStore;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        accessTokenBlocklistStore = mock(AccessTokenBlocklistStore.class);
        jwtUtil = new JwtUtil(accessTokenBlocklistStore);
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "12345678901234567890123456789012");
        jwtUtil.init();
    }

    @Test
    void blacklistedTokenBecomesInvalid() {
        String token = jwtUtil.generateToken("student@example.com", "STUDENT", 3);

        when(accessTokenBlocklistStore.isBlacklisted(token)).thenReturn(false, true);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractTokenVersion(token)).isEqualTo(3);

        jwtUtil.blacklistToken(token);
        verify(accessTokenBlocklistStore).blacklist(org.mockito.Mockito.eq(token), org.mockito.Mockito.any());

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }
}
