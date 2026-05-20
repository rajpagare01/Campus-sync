package com.campussync.backend.Controller;

import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Dto.PostRequest;
import com.campussync.backend.Dto.PostResponse;
import com.campussync.backend.Service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping({"/posts", "/api/posts", "/api/v1/posts"})
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 🏫 Only SOCIETY / DEPARTMENT / ADMIN can create posts
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public PostResponse createPost(@Valid @RequestBody PostRequest request) {
        return postService.createPost(request);
    }

    // 🌍 Public - Paginated version
    @GetMapping
    public PaginatedResponse<PostResponse> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return postService.getAllPosts(page, size);
    }

    // 🌍 Public - Keep original endpoint for backward compatibility
    @GetMapping("/all")
    public List<PostResponse> getAllPostsLegacy() {
        return postService.getAllPosts();
    }

    // 🌍 Public - view specific post
    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.updatePost(id, request);
    }

    // 👤 Get posts by specific author - Paginated version
    @GetMapping("/author/{authorId}")
    public PaginatedResponse<PostResponse> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return postService.getPostsByAuthor(authorId, page, size);
    }

    // 👤 Keep original endpoint for backward compatibility
    @GetMapping("/author/{authorId}/all")
    public List<PostResponse> getPostsByAuthorLegacy(@PathVariable Long authorId) {
        return postService.getPostsByAuthor(authorId);
    }

    // 🆕 Get posts with media (paginated)
    @GetMapping("/media")
    public PaginatedResponse<PostResponse> getPostsWithMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return postService.getPostsWithMedia(page, size);
    }

    // 🆕 Get posts linked to event (paginated)
    @GetMapping("/event/{eventId}")
    public PaginatedResponse<PostResponse> getPostsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50; // Max page size

        return postService.getPostsByEvent(eventId, page, size);
    }

    // 🏫 Author or ADMIN can delete posts
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "Post deleted successfully";
    }
}
