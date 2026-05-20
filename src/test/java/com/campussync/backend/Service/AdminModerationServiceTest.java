package com.campussync.backend.Service;

import com.campussync.backend.Dto.AdminUserStatusRequest;
import com.campussync.backend.Dto.ContentReportRequest;
import com.campussync.backend.Dto.ModerationReviewRequest;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AuditLogEntryRepository auditLogEntryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchIndexService searchIndexService;

    @InjectMocks
    private AdminModerationService adminModerationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUserStatusDeactivatesUser() {
        User admin = adminUser();
        User user = new User();
        user.setId(2L);
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);
        user.setActive(true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null)
        );

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserStatusRequest request = new AdminUserStatusRequest();
        request.setActive(false);
        request.setReason("Policy violation");

        var response = adminModerationService.updateUserStatus(2L, request);

        assertThat(response.isActive()).isFalse();
        assertThat(response.getDeactivatedAt()).isNotNull();
        assertThat(response.getDeactivationReason()).isEqualTo("Policy violation");
        verify(auditLogEntryRepository).save(any());
    }

    @Test
    void createReportPersistsOpenReportForExistingPost() {
        User reporter = new User();
        reporter.setId(3L);
        reporter.setName("Reporter");
        reporter.setEmail("reporter@example.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reporter@example.com", null)
        );

        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(postRepository.existsById(10L)).thenReturn(true);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(99L);
            return report;
        });

        ContentReportRequest request = new ContentReportRequest();
        request.setTargetType(ReportTargetType.POST);
        request.setTargetId(10L);
        request.setReason("Spam");
        request.setDetails("Repeated promotional content");

        var response = adminModerationService.createReport(request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(response.getReporterId()).isEqualTo(3L);
    }

    @Test
    void reviewReportWithRemovalDeletesReportedPostDependencies() {
        User admin = adminUser();
        Report report = new Report();
        report.setId(55L);
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(77L);
        report.setReporter(admin);
        report.setStatus(ReportStatus.OPEN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null)
        );

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(reportRepository.findById(55L)).thenReturn(Optional.of(report));
        when(postRepository.findById(77L)).thenReturn(Optional.of(new com.campussync.backend.Model.Post()));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModerationReviewRequest request = new ModerationReviewRequest();
        request.setStatus(ReportStatus.RESOLVED);
        request.setRemoveContent(true);
        request.setResolutionNotes("Removed by admin");

        var response = adminModerationService.reviewReport(55L, request);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(likeRepository).deleteByPostId(77L);
        verify(commentRepository).deleteByPostId(77L);
        verify(postRepository).deleteById(77L);
    }

    private User adminUser() {
        User admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        return admin;
    }
}
