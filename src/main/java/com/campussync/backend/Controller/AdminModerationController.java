package com.campussync.backend.Controller;

import com.campussync.backend.Dto.AdminRoleChangeRequest;
import com.campussync.backend.Dto.AdminUserStatusRequest;
import com.campussync.backend.Dto.AdminUserSummary;
import com.campussync.backend.Dto.AuditLogResponse;
import com.campussync.backend.Dto.ContentReportResponse;
import com.campussync.backend.Dto.ModerationReviewRequest;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.ReportStatus;
import com.campussync.backend.Model.ReportTargetType;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Service.AdminModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/v1/admin"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    @GetMapping("/users")
    public PaginatedResponse<AdminUserSummary> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminModerationService.getUsers(query, role, active, page, size);
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserSummary updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminRoleChangeRequest request) {
        return adminModerationService.updateUserRole(userId, request);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserSummary updateUserStatus(
            @PathVariable Long userId,
            @RequestBody AdminUserStatusRequest request) {
        return adminModerationService.updateUserStatus(userId, request);
    }

    @GetMapping("/reports")
    public PaginatedResponse<ContentReportResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminModerationService.getReports(status, targetType, page, size);
    }

    @PatchMapping("/reports/{reportId}")
    public ContentReportResponse reviewReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ModerationReviewRequest request) {
        return adminModerationService.reviewReport(reportId, request);
    }

    @GetMapping("/audit-logs")
    public PaginatedResponse<AuditLogResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminModerationService.getAuditLogs(page, size);
    }
}
