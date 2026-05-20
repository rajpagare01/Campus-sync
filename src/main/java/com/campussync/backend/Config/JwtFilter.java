package com.campussync.backend.config;

import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

            try {
                if (!jwtUtil.isTokenValid(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                int tokenVersion = jwtUtil.extractTokenVersion(token);
                User user = userRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                user = normalizeLegacyActiveState(user);

                if (!user.isActive()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                int currentTokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                if (tokenVersion != currentTokenVersion) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, token, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private User normalizeLegacyActiveState(User user) {
        if (!user.isActive()
                && user.getDeactivatedAt() == null
                && (user.getDeactivationReason() == null || user.getDeactivationReason().isBlank())) {
            user.setActive(true);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return user;
    }
}
