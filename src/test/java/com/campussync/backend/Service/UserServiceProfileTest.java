package com.campussync.backend.Service;

import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.Dto.UserProfileResponse;
import com.campussync.backend.Model.Role;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userProfileIncludesFollowStatsForCurrentViewer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer@example.com", null)
        );

        User viewer = new User();
        viewer.setId(1L);
        viewer.setEmail("viewer@example.com");
        viewer.setRole(Role.STUDENT);

        User profileUser = new User();
        profileUser.setId(2L);
        profileUser.setEmail("profile@example.com");
        profileUser.setName("Profile User");
        profileUser.setRole(Role.STUDENT);

        when(userRepository.findById(2L)).thenReturn(Optional.of(profileUser));
        when(userRepository.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(postRepository.countByAuthorId(2L)).thenReturn(4L);
        when(registrationRepository.countByUserId(2L)).thenReturn(2L);
        when(commentRepository.countByAuthorId(2L)).thenReturn(7L);
        when(followRepository.countByFollowingId(2L)).thenReturn(11L);
        when(followRepository.countByFollowerId(2L)).thenReturn(6L);
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId(2L, 1L)).thenReturn(true);

        UserProfileResponse response = userService.getUserProfile(2L);

        assertThat(response.getPostCount()).isEqualTo(4);
        assertThat(response.getEventCount()).isEqualTo(2);
        assertThat(response.getCommentCount()).isEqualTo(7);
        assertThat(response.getFollowersCount()).isEqualTo(11);
        assertThat(response.getFollowingCount()).isEqualTo(6);
        assertThat(response.isFollowing()).isTrue();
        assertThat(response.isFollowedBy()).isTrue();
        assertThat(response.isMutual()).isTrue();
    }
}
