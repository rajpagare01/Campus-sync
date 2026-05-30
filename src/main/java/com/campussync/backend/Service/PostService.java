package com.campussync.backend.Service;

import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Dto.PostRequest;
import com.campussync.backend.Dto.PostResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final LikeService likeService;
    private final CommentService commentService;
    private final RealtimeService realtimeService;
    private final SearchIndexService searchIndexService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse createPost(PostRequest request) {
        User author = getCurrentUser();
        Post post = new Post();
        post.setContent(request.getContent());
        post.setMediaUrl(request.getMediaUrl());
        post.setAuthor(author);

        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            post.setLinkedEvent(event);
        }

        Post savedPost = postRepository.save(post);
        realtimeService.broadcastFeedRefresh("POST", savedPost.getId(), "CREATED");
        searchIndexService.indexPost(savedPost);
        return mapToResponse(savedPost);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findRecentPosts(pageable);

        List<PostResponse> content = mapPostsToResponse(postPage.getContent());

        return new PaginatedResponse<>(
                content,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.isFirst(),
                postPage.isLast(),
                postPage.isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findRecentPosts();
        return mapPostsToResponse(posts);
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return mapToResponse(post);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User currentUser = getCurrentUser();

        if (!canModifyPost(post, currentUser)) {
            throw new RuntimeException("Unauthorized: You can only update your own posts");
        }

        post.setContent(request.getContent());
        if (request.getMediaUrl() != null) {
            post.setMediaUrl(request.getMediaUrl());
        }

        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            post.setLinkedEvent(event);
        }

        Post savedPost = postRepository.save(post);
        realtimeService.broadcastFeedRefresh("POST", savedPost.getId(), "UPDATED");
        searchIndexService.indexPost(savedPost);
        return mapToResponse(savedPost);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PostResponse> getPostsByAuthor(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByAuthorId(authorId, pageable);

        List<PostResponse> content = mapPostsToResponse(postPage.getContent());

        return new PaginatedResponse<>(
                content,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.isFirst(),
                postPage.isLast(),
                postPage.isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByAuthor(Long authorId) {
        List<Post> posts = postRepository.findByAuthorId(authorId);
        return mapPostsToResponse(posts);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PostResponse> getPostsWithMedia(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByMediaUrlIsNotNullOrderByCreatedAtDesc(pageable);

        List<PostResponse> content = mapPostsToResponse(postPage.getContent());

        return new PaginatedResponse<>(
                content,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.isFirst(),
                postPage.isLast(),
                postPage.isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<PostResponse> getPostsByEvent(Long eventId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByLinkedEventIdOrderByCreatedAtDesc(eventId, pageable);

        List<PostResponse> content = mapPostsToResponse(postPage.getContent());

        return new PaginatedResponse<>(
                content,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.isFirst(),
                postPage.isLast(),
                postPage.isEmpty()
        );
    }

    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User currentUser = getCurrentUser();

        if (!canModifyPost(post, currentUser)) {
            throw new RuntimeException("Unauthorized: You can only delete your own posts");
        }

        postRepository.delete(post);
        realtimeService.broadcastFeedRefresh("POST", id, "DELETED");
        searchIndexService.deletePost(id);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean canModifyPost(Post post, User currentUser) {
        return post.getAuthor().getId().equals(currentUser.getId())
                || currentUser.getRole() == Role.ADMIN;
    }

    public PostResponse mapToResponse(Post post) {
        int likes = likeService.getLikeCount(post.getId());
        int comments = commentService.getCommentCount(post.getId());
        boolean liked = likeService.hasUserLikedPost(post.getId());
        return mapToResponse(post, likes, comments, liked);
    }

    public PostResponse mapToResponse(Post post, int likes, int comments, boolean liked) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setContent(post.getContent());
        response.setMediaUrl(post.getMediaUrl());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        response.setAuthorId(post.getAuthor().getId());
        response.setAuthorName(post.getAuthor().getName());

        // Map author details for frontend compatibility
        User author = post.getAuthor();
        if (author != null) {
            String fullName = author.getName() != null ? author.getName().trim() : "";
            String[] parts = fullName.split("\\s+", 2);
            String firstName = parts.length > 0 ? parts[0] : "";
            String lastName = parts.length > 1 ? parts[1] : "";
            
            response.setAuthorFirstName(firstName);
            response.setAuthorLastName(lastName);
            response.setAuthorProfilePicture(author.getProfilePictureUrl());
            response.setAuthorProfilePictureUrl(author.getProfilePictureUrl());
            
            // Populate nested author object
            com.campussync.backend.Dto.AuthorDto authorDto = new com.campussync.backend.Dto.AuthorDto();
            authorDto.setId(author.getId());
            authorDto.setFirstName(firstName);
            authorDto.setLastName(lastName);
            authorDto.setFullName(fullName);
            authorDto.setRole(author.getRole() != null ? author.getRole().name() : null);
            authorDto.setProfilePicture(author.getProfilePictureUrl());
            authorDto.setAvatarUrl(author.getProfilePictureUrl());
            response.setAuthor(authorDto);
        }

        if (post.getLinkedEvent() != null) {
            response.setEventId(post.getLinkedEvent().getId());
            response.setEventTitle(post.getLinkedEvent().getTitle());
        }

        response.setLikeCount(likes);
        response.setLikesCount(likes);
        response.setCommentCount(comments);
        response.setCommentsCount(comments);
        response.setLikedByCurrentUser(liked);
        response.setLiked(liked);

        return response;
    }

    private List<PostResponse> mapPostsToResponse(List<Post> posts) {
        if (posts.isEmpty()) return new java.util.ArrayList<>();
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        
        java.util.Map<Long, Integer> likeMap = new java.util.HashMap<>();
        for (Object[] row : likeRepository.countByPostIdIn(postIds)) {
            likeMap.put((Long) row[0], ((Number) row[1]).intValue());
        }
        
        java.util.Map<Long, Integer> commentMap = new java.util.HashMap<>();
        for (Object[] row : commentRepository.countByPostIdIn(postIds)) {
            commentMap.put((Long) row[0], ((Number) row[1]).intValue());
        }
        
        java.util.Set<Long> userLikedPostIds = new java.util.HashSet<>();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
                if (currentUser != null) {
                    for (com.campussync.backend.Model.Like like : likeRepository.findByUserIdAndPostIdIn(currentUser.getId(), postIds)) {
                        userLikedPostIds.add(like.getPost().getId());
                    }
                }
            }
        } catch (Exception ignored) {}

        return posts.stream().map(post -> {
            int likes = likeMap.getOrDefault(post.getId(), 0);
            int comments = commentMap.getOrDefault(post.getId(), 0);
            boolean liked = userLikedPostIds.contains(post.getId());
            return mapToResponse(post, likes, comments, liked);
        }).collect(Collectors.toList());
    }
}
