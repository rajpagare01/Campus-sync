package com.campussync.backend.Service;

import com.campussync.backend.Dto.CommentRequest;
import com.campussync.backend.Dto.CommentResponse;
import com.campussync.backend.Dto.RealtimeCommentUpdate;
import com.campussync.backend.Model.Comment;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "postCommentCounts", key = "#postId")
    public CommentResponse addComment(Long postId, CommentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setParentComment(null);

        Comment savedComment = commentRepository.save(comment);
        notificationService.notifyComment(savedComment);

        CommentResponse response = mapToResponse(savedComment);
        realtimeService.broadcastCommentUpdate(toRealtimeUpdate(response));
        return response;
    }

    @Transactional
    public CommentResponse addReply(Long commentId, CommentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));

        Comment reply = new Comment();
        reply.setContent(request.getContent());
        reply.setAuthor(author);
        reply.setPost(parentComment.getPost());
        reply.setParentComment(parentComment);

        Comment savedReply = commentRepository.save(reply);
        CommentResponse response = mapToResponse(savedReply);
        realtimeService.broadcastCommentUpdate(toRealtimeUpdate(response));
        return response;
    }

    public List<CommentResponse> getPostComments(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        List<Comment> topLevelComments = commentRepository
                .findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId);

        return topLevelComments.stream()
                .map(this::buildThreadedComment)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getEmail().equals(email)) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);
        return mapToResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isOwner = comment.getAuthor().getEmail().equals(email);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You can only delete your own comments");
        }

        // Bug #5 Fix: Cascade-delete all child replies before deleting the parent.
        // Blocking deletion when replies exist is bad UX; replies should go with the parent.
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }

        commentRepository.delete(comment);
    }

    @org.springframework.cache.annotation.Cacheable(value = "postCommentCounts", key = "#postId")
    public int getCommentCount(Long postId) {
        return (int) commentRepository.countByPostId(postId);
    }

    public List<CommentResponse> getCommentReplies(Long commentId) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        return replies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse buildThreadedComment(Comment comment) {
        CommentResponse response = mapToResponse(comment);

        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::buildThreadedComment)
                .collect(Collectors.toList());

        response.setReplies(replyResponses);
        response.setReplyCount(replyResponses.size());

        return response;
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setAuthorId(comment.getAuthor().getId());
        response.setAuthorName(comment.getAuthor().getName());
        response.setPostId(comment.getPost().getId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getId());
        }

        response.setReplyCount((int) commentRepository.countByParentCommentId(comment.getId()));
        response.setReplies(new ArrayList<>());

        return response;
    }

    private RealtimeCommentUpdate toRealtimeUpdate(CommentResponse response) {
        RealtimeCommentUpdate update = new RealtimeCommentUpdate();
        update.setPostId(response.getPostId());
        update.setCommentId(response.getId());
        update.setParentCommentId(response.getParentCommentId());
        update.setContent(response.getContent());
        update.setAuthorId(response.getAuthorId());
        update.setAuthorName(response.getAuthorName());
        update.setCreatedAt(response.getCreatedAt());
        return update;
    }
}
