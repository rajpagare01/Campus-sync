package com.campussync.backend.Service;

import com.campussync.backend.Dto.LikeResponse;
import com.campussync.backend.Dto.LikeToggleResponse;
import com.campussync.backend.Model.Like;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;

    @Transactional
    public LikeToggleResponse toggleLike(Long postId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        boolean alreadyLiked = likeRepository.existsByUserIdAndPostId(user.getId(), postId);

        if (alreadyLiked) {
            likeRepository.deleteByUserIdAndPostId(user.getId(), postId);
            int likeCount = getLikeCount(postId);
            realtimeService.broadcastLikeUpdate(postId, user, false, likeCount);
            return new LikeToggleResponse(false, likeCount, null);
        }

        Like like = new Like();
        like.setUser(user);
        like.setPost(post);

        Like savedLike = likeRepository.save(like);
        notificationService.notifyLike(post, user);

        int likeCount = getLikeCount(postId);
        realtimeService.broadcastLikeUpdate(postId, user, true, likeCount);
        return new LikeToggleResponse(true, likeCount, mapToResponse(savedLike));
    }

    public List<LikeResponse> getPostLikes(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<Like> likes = likeRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return likes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public int getLikeCount(Long postId) {
        return (int) likeRepository.countByPostId(postId);
    }

    public boolean hasUserLikedPost(Long postId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return likeRepository.existsByUserIdAndPostId(user.getId(), postId);
        } catch (Exception e) {
            return false;
        }
    }

    public List<LikeResponse> getUserLikes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Like> likes = likeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return likes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LikeResponse mapToResponse(Like like) {
        LikeResponse response = new LikeResponse();
        response.setId(like.getId());
        response.setUserId(like.getUser().getId());
        response.setUserName(like.getUser().getName());
        response.setPostId(like.getPost().getId());
        response.setCreatedAt(like.getCreatedAt());
        return response;
    }
}
