package com.campussync.backend.Controller;

import com.campussync.backend.Dto.LikeResponse;
import com.campussync.backend.Dto.LikeToggleResponse;
import com.campussync.backend.Service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/posts", "/api/v1/posts"})
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * Toggle like/unlike for a post
     * POST /posts/{postId}/like
     * Returns explicit liked/unliked state and latest like count
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeToggleResponse> toggleLike(@PathVariable Long postId) {
        LikeToggleResponse response = likeService.toggleLike(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all likes for a specific post
     * GET /posts/{postId}/likes
     */
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<LikeResponse>> getPostLikes(@PathVariable Long postId) {
        List<LikeResponse> likes = likeService.getPostLikes(postId);
        return ResponseEntity.ok(likes);
    }

    /**
     * Get posts liked by current user
     * GET /posts/likes/user
     */
    @GetMapping("/likes/user")
    public ResponseEntity<List<LikeResponse>> getUserLikes() {
        List<LikeResponse> likes = likeService.getUserLikes();
        return ResponseEntity.ok(likes);
    }
}
