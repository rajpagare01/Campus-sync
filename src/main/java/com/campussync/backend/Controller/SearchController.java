package com.campussync.backend.Controller;

import com.campussync.backend.Dto.DiscoveryResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.Comment;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.campussync.backend.Dto.PostResponse;
import com.campussync.backend.Dto.UserProfileResponse;
import com.campussync.backend.Dto.EventResponse;
import com.campussync.backend.Service.PostService;
import com.campussync.backend.Service.UserService;
import com.campussync.backend.Service.EventService;
import java.util.function.Function;

@RestController
@RequestMapping({"/api/search", "/api/v1/search"})
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final PostService postService;
    private final UserService userService;
    private final EventService eventService;
    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping("/posts")
    public ResponseEntity<PaginatedResponse<PostResponse>> searchPosts(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchPosts(query, authorId, pageable), postService::mapToResponse));
    }

    @GetMapping("/events")
    public ResponseEntity<PaginatedResponse<EventResponse>> searchEvents(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(
                searchService.searchEvents(query, type, status == null ? null : status.name(), pageable),
                eventService::mapToResponse
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<PaginatedResponse<UserProfileResponse>> searchUsers(
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchUsers(query, verified, pageable), userService::mapToProfileResponseWithStats));
    }

    @GetMapping("/comments")
    public ResponseEntity<PaginatedResponse<Comment>> searchComments(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchComments(query, pageable)));
    }

    @GetMapping("/discover")
    public ResponseEntity<DiscoveryResponse> discover(
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "5") int sizePerSection) {

        int normalizedSize = Math.min(Math.max(sizePerSection, 1), 20);
        return ResponseEntity.ok(searchService.discover(query, normalizedSize));
    }

    @GetMapping("/posts/by-author/{authorId}")
    public ResponseEntity<PaginatedResponse<PostResponse>> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchPostsByAuthor(authorId, pageable), postService::mapToResponse));
    }

    @GetMapping("/events/by-location")
    public ResponseEntity<PaginatedResponse<EventResponse>> getEventsByLocation(
            @RequestParam String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchEventsByLocation(location, pageable), eventService::mapToResponse));
    }

    @GetMapping("/events/by-category")
    public ResponseEntity<PaginatedResponse<EventResponse>> getEventsByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchEventsByCategory(category, pageable), eventService::mapToResponse));
    }

    @GetMapping("/users/by-department")
    public ResponseEntity<PaginatedResponse<UserProfileResponse>> getUsersByDepartment(
            @RequestParam String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.searchUsersByDepartment(department, pageable), userService::mapToProfileResponseWithStats));
    }

    @GetMapping("/posts/published")
    public ResponseEntity<PaginatedResponse<PostResponse>> getPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.getPublishedPosts(pageable), postService::mapToResponse));
    }

    @GetMapping("/users/verified")
    public ResponseEntity<PaginatedResponse<UserProfileResponse>> getVerifiedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.getVerifiedUsers(pageable), userService::mapToProfileResponseWithStats));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<PaginatedResponse<Comment>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.getPostComments(postId, pageable)));
    }

    @GetMapping("/users/top")
    public ResponseEntity<PaginatedResponse<UserProfileResponse>> getTopUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return ResponseEntity.ok(toPaginatedResponse(searchService.getTopUsersByFollowers(pageable), userService::mapToProfileResponseWithStats));
    }

    private <T> PaginatedResponse<T> toPaginatedResponse(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    private <T, R> PaginatedResponse<R> toPaginatedResponse(Page<T> page, Function<T, R> mapper) {
        return new PaginatedResponse<>(
                page.getContent().stream().map(mapper).collect(java.util.stream.Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
