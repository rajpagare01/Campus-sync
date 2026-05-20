package com.campussync.backend.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        return toResponse(ex.getStatus(), ex.getMessage(), ex.getType());
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ProblemDetail> handleAiProviderException(AiProviderException ex) {
        return toResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), "ai-provider-error");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "validation-error");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        detail.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        return toResponse(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage(), "authentication-error");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentialsException(BadCredentialsException ex) {
        return toResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", "bad-credentials");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        return toResponse(HttpStatus.FORBIDDEN, "Access denied: You do not have permission to perform this action", "access-denied");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "Invalid argument: " + ex.getMessage(), "illegal-argument");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = determineRuntimeStatus(ex.getMessage());
        return toResponse(status,
                ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                "runtime-error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        return toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "unexpected-error");
    }

    private HttpStatus determineRuntimeStatus(String message) {
        if (message == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (normalized.contains("already")
                || normalized.contains("duplicate")
                || normalized.contains("in progress")) {
            return HttpStatus.CONFLICT;
        }
        if (message.toLowerCase().contains("deactivated")) {
            return HttpStatus.FORBIDDEN;
        }
        return isAuthenticationMessage(message)
                ? HttpStatus.UNAUTHORIZED
                : isForbiddenMessage(normalized) ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean isAuthenticationMessage(String message) {
        String normalized = message == null ? "" : message.toLowerCase();
        return normalized.contains("invalid refresh token")
                || normalized.contains("invalid password")
                || normalized.contains("please verify your email first")
                || normalized.contains("user not found");
    }

    private boolean isForbiddenMessage(String normalized) {
        return normalized.contains("unauthorized")
                || normalized.contains("only event participants")
                || normalized.contains("only registered participants")
                || normalized.contains("allowed only after attendance")
                || normalized.contains("available only for attended participants");
    }

    private ProblemDetail problem(HttpStatus status, String detail, String type) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("https://api.campussync.local/problems/" + type));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }

    private ResponseEntity<ProblemDetail> toResponse(HttpStatus status, String detail, String type) {
        return ResponseEntity.status(status).body(problem(status, detail, type));
    }
}
