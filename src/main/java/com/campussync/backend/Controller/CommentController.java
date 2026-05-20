package com.campussync.backend.Controller;

import com.campussync.backend.Dto.CommentRequest;
import com.campussync.backend.Dto.CommentResponse;
import com.campussync.backend.Service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping({"/posts", "/api/v1/posts"})
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Add a comment to a post
     * POST /posts/{postId}/comments
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {

        CommentResponse comment = commentService.addComment(postId, request);
        return ResponseEntity.ok(comment);
    }

    /**
     * Get all comments for a post (threaded)
     * GET /posts/{postId}/comments
     */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getPostComments(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getPostComments(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Update a comment (owner only)
     * PUT /comments/{commentId}
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {

        CommentResponse updatedComment = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(updatedComment);
    }

    /**
     * Delete a comment (owner or admin)
     * DELETE /comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok("Comment deleted successfully");
    }

    /**
     * Add a reply to a comment
     * POST /comments/{commentId}/replies
     */
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> addReply(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {

        CommentResponse reply = commentService.addReply(commentId, request);
        return ResponseEntity.ok(reply);
    }

    /**
     * Get replies for a specific comment
     * GET /comments/{commentId}/replies
     */
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getCommentReplies(@PathVariable Long commentId) {
        List<CommentResponse> replies = commentService.getCommentReplies(commentId);
        return ResponseEntity.ok(replies);
    }
}
