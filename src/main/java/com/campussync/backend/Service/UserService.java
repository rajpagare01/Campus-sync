package com.campussync.backend.Service;


import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.Dto.*;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.FollowRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TransientStateStore transientStateStore;
    private final JwtUtil jwtUtil;
    
    // Additional repositories for profile and activity tracking
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final RegistrationRepository registrationRepository;
    private final FollowRepository followRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final SearchIndexService searchIndexService;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN_TTL = Duration.ofSeconds(60);

    public OtpInitiationResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("Can not assign the Role of Admin");
        }

        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        transientStateStore.put(otpKey(request.getEmail()), code, OTP_TTL);
        transientStateStore.put(roleKey(request.getEmail()), request.getRole().name(), OTP_TTL);
        transientStateStore.put(tempPasswordKey(request.getEmail()), passwordEncoder.encode(request.getPassword()), OTP_TTL);
        transientStateStore.put(nameKey(request.getEmail()), request.getName(), OTP_TTL);

        emailService.sendEmail(
                request.getEmail(),
                "Verify your account",
                "Your OTP is: " + code
        );

        OtpInitiationResponse response = new OtpInitiationResponse();
        response.setMessage("OTP sent to email");
        response.setEmail(request.getEmail());
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user = normalizeLegacyActiveState(user);
        ensureUserIsActive(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        String refreshToken = generateRefreshToken();
        storeRefreshToken(user.getEmail(), refreshToken);

        return buildAuthResponse(user, refreshToken);
    }

    public AuthResponse verify(VerifyRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        String storedCode = transientStateStore.get(otpKey(email)).orElse(null);

        if (storedCode == null) {
            throw new RuntimeException("OTP expired");
        }

        if (!storedCode.equals(code)) {
            throw new RuntimeException("Invalid OTP");
        }

        String encodedPassword = transientStateStore.get(tempPasswordKey(email)).orElse(null);
        String assignRole = transientStateStore.get(roleKey(email)).orElse(null);
        String name = transientStateStore.get(nameKey(email)).orElse(null);

        if (encodedPassword == null || name == null || assignRole == null) {
            throw new RuntimeException("Session expired, register again");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setVerified(true);
        user.setRole(Role.valueOf(assignRole));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        searchIndexService.indexUser(user);

        transientStateStore.delete(
                otpKey(email),
                tempPasswordKey(email),
                roleKey(email),
                nameKey(email)
        );

        String refreshToken = generateRefreshToken();
        storeRefreshToken(email, refreshToken);

        return buildAuthResponse(user, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String existingEmail = getRefreshTokenOwner(request.getRefreshToken());

        if (existingEmail == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(existingEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user = normalizeLegacyActiveState(user);
        ensureUserIsActive(user);

        revokeRefreshToken(request.getRefreshToken(), user.getEmail());

        String newRefreshToken = generateRefreshToken();
        storeRefreshToken(user.getEmail(), newRefreshToken);

        return buildAuthResponse(user, newRefreshToken);
    }

    public String logout(LogoutRequest request) {
        LogoutRequest safeRequest = request == null ? new LogoutRequest() : request;
        String currentEmail = getCurrentAuthenticatedEmailOrNull();
        String refreshToken = safeRequest.getRefreshToken() != null ? safeRequest.getRefreshToken().trim() : null;
        String refreshTokenOwner = (refreshToken == null || refreshToken.isBlank()) ? null : getRefreshTokenOwner(refreshToken);
        invalidateAccessTokensForEmail(currentEmail != null ? currentEmail : refreshTokenOwner);
        blacklistCurrentAccessTokenIfPresent();

        if (safeRequest.isLogoutAll()) {
            if (currentEmail == null) {
                throw new RuntimeException("Unauthorized");
            }
            revokeAllRefreshTokens(currentEmail);
            return "Logged out from all sessions";
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            if (refreshTokenOwner != null) {
                revokeRefreshToken(refreshToken, refreshTokenOwner);
            } else if (currentEmail != null) {
                revokeRefreshToken(refreshToken, currentEmail);
            }
            return "Logged out";
        }

        if (currentEmail != null) {
            revokeAllRefreshTokens(currentEmail);
            return "Logged out from current account";
        }

        return "Logged out";
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        AuthResponse response = new AuthResponse();
        response.setUser(mapToDto(user));
        response.setAccessToken(jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                getTokenVersion(user)
        ));
        response.setRefreshToken(refreshToken);
        return response;
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(String.valueOf(user.getId()));
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setProfilePicture(user.getProfilePictureUrl());
        dto.setBio(user.getBio());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        String fullName = user.getName() != null ? user.getName().trim() : "";
        String[] parts = fullName.split("\\s+", 2);
        dto.setFirstName(parts.length > 0 ? parts[0] : "");
        dto.setLastName(parts.length > 1 ? parts[1] : "");

        return dto;
    }

    private void ensureUserIsActive(User user) {
        if (!user.isActive()) {
            throw new RuntimeException("User account is deactivated");
        }
    }

    private User normalizeLegacyActiveState(User user) {
        if (!user.isActive()
                && user.getDeactivatedAt() == null
                && (user.getDeactivationReason() == null || user.getDeactivationReason().isBlank())) {
            user.setActive(true);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return user;
    }

    private void storeRefreshToken(String email, String refreshToken) {
        refreshTokenStore.store(email, refreshToken);
    }

    private void revokeRefreshToken(String refreshToken, String email) {
        refreshTokenStore.revoke(email, refreshToken);
    }

    private void revokeAllRefreshTokens(String email) {
        refreshTokenStore.revokeAll(email);
    }

    private String getRefreshTokenOwner(String refreshToken) {
        return refreshTokenStore.findOwner(refreshToken).orElse(null);
    }

    private String getCurrentAuthenticatedEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    private void blacklistCurrentAccessTokenIfPresent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            return;
        }

        Object credentials = auth.getCredentials();
        if (credentials instanceof String token && !token.isBlank()) {
            try {
                jwtUtil.blacklistToken(token);
            } catch (Exception ignored) {
                // Best-effort access-token revocation if Redis is available.
            }
        }
    }

    private void invalidateAccessTokensForEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setTokenVersion(getTokenVersion(user) + 1);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    private int getTokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    public String resendOtp(String email) {

        //  Check cooldown
        boolean exists = transientStateStore.exists(cooldownKey(email));

        if (exists) {
            throw new RuntimeException("Please wait before requesting another OTP");
        }

        // 🔥 Generate new OTP
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);

        // Store OTP again (5 min)
        transientStateStore.put(otpKey(email), code, OTP_TTL);
        transientStateStore.put(cooldownKey(email), "LOCK", RESEND_COOLDOWN_TTL);

        // Send email
        emailService.sendEmail(
                email,
                "Resend OTP",
                "Your new OTP is: " + code
        );

        return "OTP resent successfully";
    }
    public String forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        // Store OTP (5 min)
        transientStateStore.put(resetOtpKey(email), otp, OTP_TTL);

        emailService.sendEmail(
                email,
                "Reset Password",
                "Your OTP for password reset is: " + otp
        );

        return "OTP sent to email";
    }

    public String resetPassword(String email, String otp, String newPassword) {

        String storedOtp = transientStateStore.get(resetOtpKey(email)).orElse(null);

        if (storedOtp == null) {
            throw new RuntimeException("OTP expired");
        }

        if (!storedOtp.equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(getTokenVersion(user) + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        searchIndexService.indexUser(user);

        transientStateStore.delete(resetOtpKey(email));

        return "Password reset successful";
    }

    // 🆕 PROFILE MANAGEMENT METHODS WITH CACHING

    /**
     * Get user profile with activity and follow statistics.
     * Follow relationship flags depend on the current viewer, so this method is not cached by userId.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToProfileResponseWithStats(user);
    }

    public UserProfileResponse mapToProfileResponseWithStats(User user) {
        UserProfileResponse profile = mapToProfileResponse(user);
        
        // Add activity statistics
        profile.setPostCount((int) postRepository.countByAuthorId(user.getId()));
        profile.setEventCount((int) registrationRepository.countByUserId(user.getId()));
        profile.setCommentCount((int) commentRepository.countByAuthorId(user.getId()));
        profile.setLikeCount((int) likeRepository.countByUserId(user.getId()));
        populateFollowStats(profile, user.getId());

        return profile;
    }

    /**
     * Get current user's profile
     * Uses same cache as getUserProfile
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getUserProfile(user.getId());
    }

    /**
     * Update user profile information
     * Evicts cache to ensure fresh data
     */
    @CacheEvict(value = "userProfiles", key = "#result.id")
    public UserProfileResponse updateProfile(UserProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update profile fields
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getYear() != null) {
            user.setYear(request.getYear());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        searchIndexService.indexUser(updatedUser);

        return getUserProfile(updatedUser.getId());
    }

    /**
     * Get user's activity feed
     * Not cached as activity changes frequently
     */
    @Transactional(readOnly = true)
    public List<UserActivityResponse> getUserActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserActivityResponse> activities = new ArrayList<>();

        // Get posts by user
        postRepository.findByAuthorId(userId).forEach(post -> {
            UserActivityResponse activity = new UserActivityResponse();
            activity.setActivityType("POST");
            activity.setDescription("Created a post");
            activity.setTimestamp(post.getCreatedAt());
            activity.setRelatedId(post.getId());
            activities.add(activity);
        });

        // Get comments by user
        commentRepository.findByAuthorIdOrderByCreatedAtDesc(userId).forEach(comment -> {
            UserActivityResponse activity = new UserActivityResponse();
            activity.setActivityType("COMMENT");
            activity.setDescription("Commented on a post");
            activity.setTimestamp(comment.getCreatedAt());
            activity.setRelatedId(comment.getPost().getId());
            activities.add(activity);
        });

        // Get likes by user
        likeRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(like -> {
            UserActivityResponse activity = new UserActivityResponse();
            activity.setActivityType("LIKE");
            activity.setDescription("Liked a post");
            activity.setTimestamp(like.getCreatedAt());
            activity.setRelatedId(like.getPost().getId());
            activities.add(activity);
        });

        // Get registrations by user
        registrationRepository.findByUserId(userId).forEach(reg -> {
            UserActivityResponse activity = new UserActivityResponse();
            activity.setActivityType("REGISTRATION");
            activity.setDescription("Registered for event: " + reg.getEvent().getTitle());
            activity.setTimestamp(
                    reg.getCreatedAt() != null
                            ? reg.getCreatedAt()
                            : reg.getEvent().getCreatedAt() != null
                                    ? reg.getEvent().getCreatedAt()
                                    : reg.getEvent().getDate()
            );
            activity.setRelatedId(reg.getEvent().getId());
            activity.setRelatedTitle(reg.getEvent().getTitle());
            activities.add(activity);
        });

        // Sort by timestamp descending (newest first)
        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return activities;
    }

    /**
     * Get current user's activity
     */
    public List<UserActivityResponse> getMyActivity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return getUserActivity(user.getId());
    }

    /**
     * Update profile picture
     * Evicts cache to ensure fresh data
     */
    @CacheEvict(value = "userProfiles", key = "#result.id")
    public UserProfileResponse updateProfilePicture(String profilePictureUrl) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfilePictureUrl(profilePictureUrl);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        searchIndexService.indexUser(updatedUser);

        return getUserProfile(updatedUser.getId());
    }

    /**
     * Get user by ID (public profile)
     * Uses same cache as getUserProfile
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(Long userId) {
        return getUserProfile(userId);
    }

    // Helper method to map User entity to UserProfileResponse
    private UserProfileResponse mapToProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setBio(user.getBio());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setDepartment(user.getDepartment());
        response.setYear(user.getYear());
        response.setVerified(user.isVerified());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private void populateFollowStats(UserProfileResponse profile, Long profileUserId) {
        profile.setFollowersCount(toIntCount(followRepository.countByFollowingId(profileUserId)));
        profile.setFollowingCount(toIntCount(followRepository.countByFollowerId(profileUserId)));

        User currentUser = getCurrentAuthenticatedUserOrNull();
        if (currentUser == null || currentUser.getId().equals(profileUserId)) {
            profile.setFollowing(false);
            profile.setFollowedBy(false);
            profile.setMutual(false);
            return;
        }

        boolean currentUserFollowsProfile = followRepository.existsByFollowerIdAndFollowingId(
                currentUser.getId(),
                profileUserId
        );
        boolean profileFollowsCurrentUser = followRepository.existsByFollowerIdAndFollowingId(
                profileUserId,
                currentUser.getId()
        );

        profile.setFollowing(currentUserFollowsProfile);
        profile.setFollowedBy(profileFollowsCurrentUser);
        profile.setMutual(currentUserFollowsProfile && profileFollowsCurrentUser);
    }

    private User getCurrentAuthenticatedUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private int toIntCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private String otpKey(String email) {
        return "OTP_" + email;
    }

    private String roleKey(String email) {
        return "ROLE_" + email;
    }

    private String tempPasswordKey(String email) {
        return "TEMP_" + email;
    }

    private String nameKey(String email) {
        return "NAME_" + email;
    }

    private String cooldownKey(String email) {
        return "COOLDOWN_" + email;
    }

    private String resetOtpKey(String email) {
        return "RESET_OTP_" + email;
    }
}
