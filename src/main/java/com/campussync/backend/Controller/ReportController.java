package com.campussync.backend.Controller;

import com.campussync.backend.Dto.ContentReportRequest;
import com.campussync.backend.Dto.ContentReportResponse;
import com.campussync.backend.Service.AdminModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/reports", "/api/v1/reports"})
@RequiredArgsConstructor
public class ReportController {

    private final AdminModerationService adminModerationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ContentReportResponse createReport(@Valid @RequestBody ContentReportRequest request) {
        return adminModerationService.createReport(request);
    }
}
