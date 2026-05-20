package com.campussync.backend.Service;

import com.campussync.backend.Dto.AdminRoleChangeRequest;
import com.campussync.backend.Dto.AdminUserStatusRequest;
import com.campussync.backend.Dto.AdminUserSummary;
import com.campussync.backend.Dto.AuditLogResponse;
import com.campussync.backend.Dto.ContentReportRequest;
import com.campussync.backend.Dto.ContentReportResponse;
import com.campussync.backend.Dto.ModerationReviewRequest;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.AuditLogEntry;
import com.campussync.backend.Model.Comment;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.Report;
import com.campussync.backend.Model.ReportStatus;
import com.campussync.backend.Model.ReportTargetType;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.AuditLogEntryRepository;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.LikeRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.ReportRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final LikeRepository likeRepository;
    private final RegistrationRepository registrationRepository;
    private final AuditService auditService;
    private final SearchIndexService searchIndexService;

    @Transactional(readOnly = true)
    public PaginatedResponse<AdminUserSummary> getUsers(String query, Role role, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.searchForAdmin(normalize(query), role, active, pageable);
        List<AdminUserSummary> content = userPage.getContent().stream().map(this::mapUser).toList();
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

    @Transactional
    public AdminUserSummary updateUserRole(Long userId, AdminRoleChangeRequest request) {
        User admin = getCurrentUser();
        User user = findUser(userId);
        Role previousRole = user.getRole();
        user.setRole(request.getRole());
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        searchIndexService.indexUser(saved);
        saveAudit(admin, "USER_ROLE_CHANGED", "USER", userId,
                "Role changed from " + previousRole + " to " + request.getRole());
        return mapUser(saved);
    }

    @Transactional
    public AdminUserSummary updateUserStatus(Long userId, AdminUserStatusRequest request) {
        User admin = getCurrentUser();
        User user = findUser(userId);
        user.setActive(request.isActive());
        user.setUpdatedAt(LocalDateTime.now());

        if (request.isActive()) {
            user.setDeactivatedAt(null);
            user.setDeactivationReason(null);
            saveAudit(admin, "USER_REACTIVATED", "USER", userId, "User reactivated");
        } else {
            user.setDeactivatedAt(LocalDateTime.now());
            user.setDeactivationReason(request.getReason());
            saveAudit(admin, "USER_DEACTIVATED", "USER", userId,
                    "User deactivated. Reason: " + safe(request.getReason()));
        }
        User saved = userRepository.save(user);
        searchIndexService.indexUser(saved);
        return mapUser(saved);
    }

    @Transactional
    public ContentReportResponse createReport(ContentReportRequest request) {
        User reporter = getCurrentUser();
        validateReportTargetExists(request.getTargetType(), request.getTargetId());

        Report report = new Report();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());
        report.setReporter(reporter);
        report.setStatus(ReportStatus.OPEN);

        Report saved = reportRepository.save(report);
        saveAudit(reporter, "REPORT_CREATED", request.getTargetType().name(), request.getTargetId(),
                "Report #" + saved.getId() + " created with reason: " + request.getReason());
        return mapReport(saved);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ContentReportResponse> getReports(ReportStatus status,
                                                               ReportTargetType targetType,
                                                               int page,
                                                               int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reportPage = reportRepository.findForModeration(status, targetType, pageable);
        List<ContentReportResponse> content = reportPage.getContent().stream().map(this::mapReport).toList();
        return new PaginatedResponse<>(
                content,
                reportPage.getNumber(),
                reportPage.getSize(),
                reportPage.getTotalElements(),
                reportPage.getTotalPages(),
                reportPage.isFirst(),
                reportPage.isLast(),
                reportPage.isEmpty()
        );
    }

    @Transactional
    public ContentReportResponse reviewReport(Long reportId, ModerationReviewRequest request) {
        User admin = getCurrentUser();
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (request.isRemoveContent() && request.getStatus() != ReportStatus.DISMISSED) {
            removeContent(report.getTargetType(), report.getTargetId());
        }

        report.setStatus(request.getStatus());
        report.setResolutionNotes(request.getResolutionNotes());
        report.setReviewer(admin);
        report.setReviewedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);
        saveAudit(admin, "REPORT_REVIEWED", report.getTargetType().name(), report.getTargetId(),
                "Report #" + reportId + " marked " + request.getStatus()
                        + (request.isRemoveContent() ? " with content removal" : ""));
        return mapReport(saved);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<AuditLogResponse> getAuditLogs(int page, int size) {
        Page<AuditLogEntry> auditPage = auditLogEntryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        List<AuditLogResponse> content = auditPage.getContent().stream().map(this::mapAudit).toList();
        return new PaginatedResponse<>(
                content,
                auditPage.getNumber(),
                auditPage.getSize(),
                auditPage.getTotalElements(),
                auditPage.getTotalPages(),
                auditPage.isFirst(),
                auditPage.isLast(),
                auditPage.isEmpty()
        );
    }

    @Transactional
    protected void removeContent(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> removePost(targetId);
            case COMMENT -> removeComment(targetId);
            case EVENT -> removeEvent(targetId);
            default -> throw new IllegalArgumentException("Unsupported report target type");
        }
    }

    private void removePost(Long postId) {
        postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        likeRepository.deleteByPostId(postId);

        // Bug #14 Fix: delete child replies before parent comments to avoid FK constraint violations.
        // Fetch top-level comments for this post and recursively remove their reply trees first.
        List<Comment> topLevel = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId);
        for (Comment comment : topLevel) {
            deleteCommentReplies(comment.getId());
        }
        commentRepository.deleteByPostId(postId);

        postRepository.deleteById(postId);
        searchIndexService.deletePost(postId);
    }

    private void removeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        deleteCommentReplies(comment.getId());
        commentRepository.deleteById(comment.getId());
    }

    private void deleteCommentReplies(Long parentId) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentId);
        for (Comment reply : replies) {
            deleteCommentReplies(reply.getId());
        }
        commentRepository.deleteByParentCommentId(parentId);
    }

    private void removeEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        List<Post> linkedPosts = postRepository.findByLinkedEventIdOrderByCreatedAtDesc(eventId);
        linkedPosts.forEach(post -> post.setLinkedEvent(null));
        postRepository.saveAll(linkedPosts);
        registrationRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
        searchIndexService.deleteEvent(eventId);
    }

    private void validateReportTargetExists(ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case EVENT -> eventRepository.existsById(targetId);
        };

        if (!exists) {
            throw new RuntimeException(targetType + " not found");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("User not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void saveAudit(User actor, String action, String entityType, Long entityId, String details) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogEntryRepository.save(entry);
        auditService.logDataModificationEvent(entityType, action, entityId, details);
    }

    private AdminUserSummary mapUser(User user) {
        AdminUserSummary dto = new AdminUserSummary();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setVerified(user.isVerified());
        dto.setActive(user.isActive());
        dto.setBio(user.getBio());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setDeactivatedAt(user.getDeactivatedAt());
        dto.setDeactivationReason(user.getDeactivationReason());
        return dto;
    }

    private ContentReportResponse mapReport(Report report) {
        ContentReportResponse dto = new ContentReportResponse();
        dto.setId(report.getId());
        dto.setTargetType(report.getTargetType());
        dto.setTargetId(report.getTargetId());
        dto.setReason(report.getReason());
        dto.setDetails(report.getDetails());
        dto.setStatus(report.getStatus());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterName(report.getReporter().getName());
        if (report.getReviewer() != null) {
            dto.setReviewerId(report.getReviewer().getId());
            dto.setReviewerName(report.getReviewer().getName());
        }
        dto.setResolutionNotes(report.getResolutionNotes());
        dto.setCreatedAt(report.getCreatedAt());
        dto.setReviewedAt(report.getReviewedAt());
        return dto;
    }

    private AuditLogResponse mapAudit(AuditLogEntry entry) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(entry.getId());
        dto.setAction(entry.getAction());
        dto.setEntityType(entry.getEntityType());
        dto.setEntityId(entry.getEntityId());
        dto.setDetails(entry.getDetails());
        dto.setCreatedAt(entry.getCreatedAt());
        if (entry.getActor() != null) {
            dto.setActorId(entry.getActor().getId());
            dto.setActorName(entry.getActor().getName());
        }
        return dto;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "No reason provided" : value.trim();
    }
}
