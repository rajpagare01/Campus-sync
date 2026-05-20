package com.campussync.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/auth/**", "/events/**", "/posts/**", "/users/**", "/feed/**", "/registrations/**", "/api/ai/**");
    }

    /**
     * Rate Limit Interceptor
     * Limits requests per minute based on IP address
     */
    public static class RateLimitInterceptor implements HandlerInterceptor {

        private static final Map<String, Bucket> cache = new ConcurrentHashMap<>();
        private static final long RATE_LIMIT_CAPACITY = 100; // 100 requests
        private static final Duration RATE_LIMIT_DURATION = Duration.ofMinutes(1); // per minute

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String key = getRateLimitKey(request);
            
            // Special handling for auth endpoints - stricter limits
            String path = request.getRequestURI();
            if (path.contains("/auth/")) {
                Bucket bucket = cache.computeIfAbsent(key + "_auth", k -> createBucket(5, Duration.ofMinutes(1)));
                if (!bucket.tryConsume(1)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setHeader("X-RateLimit-Retry-After-Seconds", "60");
                    response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                    return false;
                }
            } else if (path.contains("/api/ai/")) {
                Bucket bucket = cache.computeIfAbsent(key + "_ai", k -> createBucket(5, Duration.ofMinutes(1)));
                if (!bucket.tryConsume(1)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setHeader("X-RateLimit-Retry-After-Seconds", "60");
                    response.getWriter().write("{\"error\": \"AI rate limit exceeded. Please try again later.\"}");
                    return false;
                }
            } else {
                // 100 requests per minute for other endpoints
                Bucket bucket = cache.computeIfAbsent(key, k -> createBucket(RATE_LIMIT_CAPACITY, RATE_LIMIT_DURATION));
                if (!bucket.tryConsume(1)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setHeader("X-RateLimit-Retry-After-Seconds", "60");
                    response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
                    return false;
                }
            }

            return true;
        }

        private Bucket createBucket(long capacity, Duration duration) {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, duration));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        private String getRateLimitKey(HttpServletRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null
                    && !"anonymousUser".equals(authentication.getName())) {
                return "user:" + authentication.getName();
            }

            String emailHint = request.getParameter("email");
            if (emailHint != null && !emailHint.isBlank()) {
                return "email:" + emailHint.trim().toLowerCase();
            }

            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return "ip:" + xForwardedFor.split(",")[0].trim();
            }
            return "ip:" + request.getRemoteAddr();
        }
    }
}
