package com.campussync.backend.Service;

import com.campussync.backend.Dto.FollowResponse;
import com.campussync.backend.Dto.FollowStats;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.Follow;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.FollowRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    /**
     * Follow a user
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userProfiles", allEntries = true),
            @CacheEvict(value = "followStats", allEntries = true)
    })
    public FollowResponse followUser(Long targetUserId) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)) {
            throw new IllegalArgumentException("Already following this user");
        }

        Follow follow = new Follow();
        follow.setFollower(currentUser);
        follow.setFollowing(targetUser);
        follow.setCreatedAt(LocalDateTime.now());

        Follow savedFollow = followRepository.save(follow);

        // Audit logging
        auditService.logDataModificationEvent("FOLLOW", "CREATE", savedFollow.getId(),
                "User " + currentUser.getId() + " followed user " + targetUserId);
        
        // Notify the followed user
        notificationService.notifyFollow(currentUser, targetUser);

        return mapToResponse(savedFollow);
    }

    /**
     * Unfollow a user
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userProfiles", allEntries = true),
            @CacheEvict(value = "followStats", allEntries = true)
    })
    public String unfollowUser(Long targetUserId) {
        User currentUser = getCurrentUser();

        Follow follow = followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)
                .orElseThrow(() -> new RuntimeException("Not following this user"));

        followRepository.delete(follow);

        // Audit logging
        auditService.logDataModificationEvent("FOLLOW", "DELETE", follow.getId(),
                "User " + currentUser.getId() + " unfollowed user " + targetUserId);

        return "Successfully unfollowed user";
    }

    /**
     * Check if current user follows target user
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long targetUserId) {
        User currentUser = getCurrentUser();
        return followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId);
    }

    /**
     * Get follow statistics for a user
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "followStats", key = "#userId")
    public FollowStats getFollowStats(Long userId) {
        long followersCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);

        // Follow relationship flags require an authenticated viewer
        User currentUser = getCurrentUserOrNull();
        boolean isFollowing = false;
        boolean isFollowedBy = false;
        boolean isMutual = false;

        if (currentUser != null && !currentUser.getId().equals(userId)) {
            isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), userId);
            isFollowedBy = followRepository.existsByFollowerIdAndFollowingId(userId, currentUser.getId());
            isMutual = isFollowing && isFollowedBy;
        }

        return new FollowStats(followersCount, followingCount, isFollowing, isFollowedBy, isMutual);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<FollowResponse> getFollowers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findFollowersByUserId(userId, pageable);

        List<FollowResponse> content = followPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                followPage.getNumber(),
                followPage.getSize(),
                followPage.getTotalElements(),
                followPage.getTotalPages(),
                followPage.isFirst(),
                followPage.isLast(),
                followPage.isEmpty()
        );
    }

    /**
     * Get users that a user is following (paginated)
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<FollowResponse> getFollowing(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> followPage = followRepository.findFollowingByUserId(userId, pageable);

        List<FollowResponse> content = followPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                followPage.getNumber(),
                followPage.getSize(),
                followPage.getTotalElements(),
                followPage.getTotalPages(),
                followPage.isFirst(),
                followPage.isLast(),
                followPage.isEmpty()
        );
    }

    /**
     * Get list of IDs that a user is following
     */
    @Transactional(readOnly = true)
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFollowingIdsByUserId(userId);
    }

    /**
     * Get recommended users to follow
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<com.campussync.backend.Dto.AuthorDto> getRecommendedUsers(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = followRepository.findRecommendedUsersToFollow(currentUser.getId(), pageable);
        
        // If no recommendations from network, fallback to popular users
        if (userPage.isEmpty() && page == 0) {
            userPage = followRepository.findPopularUsersToFollow(currentUser.getId(), pageable);
        }

        List<com.campussync.backend.Dto.AuthorDto> content = userPage.getContent().stream()
                .map(this::mapToAuthorDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isFirst(),
                userPage.isLast(),
                userPage.isEmpty()
        );
    }

    /**
     * Get mutual follows (users who follow each other)
     */
    @Transactional(readOnly = true)
    public List<com.campussync.backend.Dto.AuthorDto> getMutualFollows() {
        User currentUser = getCurrentUser();
        return followRepository.findMutualFollows(currentUser.getId()).stream()
                .map(this::mapToAuthorDto)
                .collect(Collectors.toList());
    }

    /**
     * Get follower users (for profile display)
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<com.campussync.backend.Dto.AuthorDto> getFollowerUsers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = followRepository.findFollowerUsersByUserId(userId, pageable);

        List<com.campussync.backend.Dto.AuthorDto> content = userPage.getContent().stream()
                .map(this::mapToAuthorDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isFirst(),
                userPage.isLast(),
                userPage.isEmpty()
        );
    }

    /**
     * Get following users (for profile display)
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<com.campussync.backend.Dto.AuthorDto> getFollowingUsers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = followRepository.findFollowingUsersByUserId(userId, pageable);

        List<com.campussync.backend.Dto.AuthorDto> content = userPage.getContent().stream()
                .map(this::mapToAuthorDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isFirst(),
                userPage.isLast(),
                userPage.isEmpty()
        );
    }

    private com.campussync.backend.Dto.AuthorDto mapToAuthorDto(User user) {
        com.campussync.backend.Dto.AuthorDto dto = new com.campussync.backend.Dto.AuthorDto();
        dto.setId(user.getId());
        String fullName = user.getName() != null ? user.getName().trim() : "";
        String[] parts = fullName.split("\\s+", 2);
        dto.setFirstName(parts.length > 0 ? parts[0] : "");
        dto.setLastName(parts.length > 1 ? parts[1] : "");
        dto.setFullName(fullName);
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setProfilePicture(user.getProfilePictureUrl());
        dto.setAvatarUrl(user.getProfilePictureUrl());
        return dto;
    }

    // Helper methods

    private FollowResponse mapToResponse(Follow follow) {
        boolean isMutual = followRepository.existsByFollowerIdAndFollowingId(follow.getFollowing().getId(), follow.getFollower().getId());

        return new FollowResponse(
                follow.getId(),
                follow.getFollower().getId(),
                follow.getFollower().getName(),
                follow.getFollowing().getId(),
                follow.getFollowing().getName(),
                follow.getCreatedAt(),
                isMutual
        );
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private User getCurrentUserOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
                return null;
            }
            return userRepository.findByEmail(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
