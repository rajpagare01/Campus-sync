package com.campussync.backend.Controller;

import com.campussync.backend.Dto.FollowResponse;
import com.campussync.backend.Dto.FollowStats;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.User;
import com.campussync.backend.Service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/follow", "/api/v1/follow"})
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * Follow a user
     */
    @PostMapping("/follow/{userId}")
    public ResponseEntity<FollowResponse> followUser(@PathVariable Long userId) {
        FollowResponse response = followService.followUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/unfollow/{userId}")
    public ResponseEntity<String> unfollowUser(@PathVariable Long userId) {
        String message = followService.unfollowUser(userId);
        return ResponseEntity.ok(message);
    }

    /**
     * Check if current user follows target user
     */
    @GetMapping("/is-following/{userId}")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long userId) {
        boolean isFollowing = followService.isFollowing(userId);
        return ResponseEntity.ok(isFollowing);
    }

    /**
     * Get follow statistics for a user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<FollowStats> getFollowStats(@PathVariable Long userId) {
        FollowStats stats = followService.getFollowStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get followers of a user (paginated)
     */
    @GetMapping("/followers/{userId}")
    public ResponseEntity<PaginatedResponse<FollowResponse>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<FollowResponse> response = followService.getFollowers(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get users that a user is following (paginated)
     */
    @GetMapping("/following/{userId}")
    public ResponseEntity<PaginatedResponse<FollowResponse>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<FollowResponse> response = followService.getFollowing(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recommended users to follow (paginated)
     */
    @GetMapping("/recommendations")
    public ResponseEntity<PaginatedResponse<com.campussync.backend.Dto.AuthorDto>> getRecommendedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<com.campussync.backend.Dto.AuthorDto> response = followService.getRecommendedUsers(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get mutual follows (users who follow each other)
     */
    @GetMapping("/mutual")
    public ResponseEntity<List<com.campussync.backend.Dto.AuthorDto>> getMutualFollows() {
        List<com.campussync.backend.Dto.AuthorDto> mutualFollows = followService.getMutualFollows();
        return ResponseEntity.ok(mutualFollows);
    }

    /**
     * Get follower users (for profile display, paginated)
     */
    @GetMapping("/followers/users/{userId}")
    public ResponseEntity<PaginatedResponse<com.campussync.backend.Dto.AuthorDto>> getFollowerUsers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<com.campussync.backend.Dto.AuthorDto> response = followService.getFollowerUsers(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get following users (for profile display, paginated)
     */
    @GetMapping("/following/users/{userId}")
    public ResponseEntity<PaginatedResponse<com.campussync.backend.Dto.AuthorDto>> getFollowingUsers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<com.campussync.backend.Dto.AuthorDto> response = followService.getFollowingUsers(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
