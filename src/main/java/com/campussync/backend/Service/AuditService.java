package com.campussync.backend.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log authentication event (login/logout)
     */
    public void logAuthenticationEvent(String email, String action, String status, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String log = String.format(
                "[%s] AUTH_EVENT | action=%s | email=%s | status=%s | details=%s",
                timestamp, action, email, status, details
        );
        if ("SUCCESS".equals(status)) {
            auditLogger.info(log);
        } else {
            auditLogger.warn(log);
        }
    }

    /**
     * Log authorization event (access attempt)
     */
    public void logAuthorizationEvent(String resource, String action, String status, String reason) {
        String email = getCurrentUserEmail();
        String timestamp = LocalDateTime.now().format(formatter);
        String log = String.format(
                "[%s] AUTHZ_EVENT | user=%s | resource=%s | action=%s | status=%s | reason=%s",
                timestamp, email, resource, action, status, reason
        );
        if ("ALLOWED".equals(status)) {
            auditLogger.info(log);
        } else {
            auditLogger.warn(log);
        }
    }

    /**
     * Log data modification event
     */
    public void logDataModificationEvent(String entity, String action, Long entityId, String changes) {
        String email = getCurrentUserEmail();
        String timestamp = LocalDateTime.now().format(formatter);
        String log = String.format(
                "[%s] DATA_MOD | user=%s | entity=%s | action=%s | entityId=%d | changes=%s",
                timestamp, email, entity, action, entityId, changes
        );
        auditLogger.info(log);
    }

    /**
     * Log security event (potential threats)
     */
    public void logSecurityEvent(String eventType, String details, String severity) {
        String email = getCurrentUserEmail();
        String timestamp = LocalDateTime.now().format(formatter);
        String log = String.format(
                "[%s] SECURITY_EVENT | severity=%s | type=%s | user=%s | details=%s",
                timestamp, severity, eventType, email, details
        );
        if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
            auditLogger.error(log);
        } else {
            auditLogger.warn(log);
        }
    }

    /**
     * Log validation failure
     */
    public void logValidationFailure(String endpoint, String errors) {
        String email = getCurrentUserEmail();
        String timestamp = LocalDateTime.now().format(formatter);
        String log = String.format(
                "[%s] VALIDATION_FAILURE | user=%s | endpoint=%s | errors=%s",
                timestamp, email, endpoint, errors
        );
        auditLogger.warn(log);
    }

    /**
     * Get current authenticated user email
     */
    private String getCurrentUserEmail() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception e) {
            // User not authenticated
        }
        return "ANONYMOUS";
    }
}
