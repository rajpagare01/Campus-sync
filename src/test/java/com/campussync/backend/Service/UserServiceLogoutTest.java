package com.campussync.backend.Service;

import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.Dto.LogoutRequest;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.FollowRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceLogoutTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private TransientStateStore transientStateStore;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private SearchIndexService searchIndexService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutRevokesProvidedRefreshToken() {
        when(refreshTokenStore.findOwner("token-123")).thenReturn(Optional.of("student@example.com"));
        User user = new User();
        user.setEmail("student@example.com");
        user.setTokenVersion(0);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("token-123");

        String result = userService.logout(request);

        assertThat(result).isEqualTo("Logged out");
        verify(refreshTokenStore).revoke("student@example.com", "token-123");
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getTokenVersion() != null && savedUser.getTokenVersion() == 1
        ));
    }

    @Test
    void logoutAllRevokesAllStoredTokensForCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        User user = new User();
        user.setEmail("student@example.com");
        user.setTokenVersion(4);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        LogoutRequest request = new LogoutRequest();
        request.setLogoutAll(true);

        String result = userService.logout(request);

        assertThat(result).isEqualTo("Logged out from all sessions");
        verify(refreshTokenStore).revokeAll("student@example.com");
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getTokenVersion() != null && savedUser.getTokenVersion() == 5
        ));
    }
}
