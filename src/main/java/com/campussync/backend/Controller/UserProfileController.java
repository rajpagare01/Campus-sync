package com.campussync.backend.Controller;

import com.campussync.backend.Dto.UserActivityResponse;
import com.campussync.backend.Dto.UserProfileRequest;
import com.campussync.backend.Dto.UserProfileResponse;
import com.campussync.backend.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/users", "/api/v1/users"})
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    /**
     * Get current user's profile
     * Endpoint: GET /users/profile
     * Authentication: Required (All authenticated users)
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        try {
            UserProfileResponse profile = userService.getMyProfile();
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Get specific user's public profile
     * Endpoint: GET /users/{userId}/profile
     * Authentication: Not required (Public endpoint)
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        try {
            UserProfileResponse profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Update current user's profile
     * Endpoint: PUT /users/profile
     * Authentication: Required (All authenticated users)
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        try {
            UserProfileResponse updatedProfile = userService.updateProfile(request);
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Update profile picture
     * Endpoint: PATCH /users/profile/picture
     * Authentication: Required
     */
    @PatchMapping("/profile/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfilePicture(
            @RequestParam String pictureUrl) {
        try {
            UserProfileResponse updatedProfile = userService.updateProfilePicture(pictureUrl);
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Get current user's activity
     * Endpoint: GET /users/activity
     * Authentication: Required (All authenticated users)
     */
    @GetMapping("/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserActivityResponse>> getMyActivity() {
        try {
            List<UserActivityResponse> activities = userService.getMyActivity();
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Get specific user's activity
     * Endpoint: GET /users/{userId}/activity
     * Authentication: Not required (Public endpoint)
     */
    @GetMapping("/{userId}/activity")
    public ResponseEntity<List<UserActivityResponse>> getUserActivity(@PathVariable Long userId) {
        try {
            List<UserActivityResponse> activities = userService.getUserActivity(userId);
            return ResponseEntity.ok(activities);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Get user statistics summary
     * Endpoint: GET /users/{userId}/stats
     * Authentication: Not required (Public endpoint)
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        try {
            UserProfileResponse profile = userService.getUserProfile(userId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("userId", profile.getId());
            stats.put("userName", profile.getName());
            stats.put("postCount", profile.getPostCount());
            stats.put("eventRegistrations", profile.getEventCount());
            stats.put("likeCount", profile.getLikeCount());
            stats.put("commentCount", profile.getCommentCount());
            stats.put("joinedDate", profile.getCreatedAt());
            
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Get current user's statistics summary
     * Endpoint: GET /users/stats/my-stats
     * Authentication: Required
     */
    @GetMapping("/stats/my-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyStats() {
        try {
            UserProfileResponse profile = userService.getMyProfile();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("userId", profile.getId());
            stats.put("userName", profile.getName());
            stats.put("postCount", profile.getPostCount());
            stats.put("eventRegistrations", profile.getEventCount());
            stats.put("likeCount", profile.getLikeCount());
            stats.put("commentCount", profile.getCommentCount());
            stats.put("joinedDate", profile.getCreatedAt());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
