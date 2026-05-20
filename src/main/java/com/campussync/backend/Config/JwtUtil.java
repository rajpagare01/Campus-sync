package com.campussync.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import com.campussync.backend.Service.AccessTokenBlocklistStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final AccessTokenBlocklistStore accessTokenBlocklistStore;
    private Key key;

    public JwtUtil(AccessTokenBlocklistStore accessTokenBlocklistStore) {
        this.accessTokenBlocklistStore = accessTokenBlocklistStore;
    }

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        return generateToken(email, role, 0);
    }

    public String generateToken(String email, String role, int tokenVersion) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public int extractTokenVersion(String token) {
        Integer tokenVersion = extractAllClaims(token).get("tokenVersion", Integer.class);
        return tokenVersion == null ? 0 : tokenVersion;
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public void blacklistToken(String token) {
        Date expiration = extractExpiration(token);
        Instant expiresAt = expiration.toInstant();
        if (!expiresAt.isAfter(Instant.now())) {
            return;
        }
        accessTokenBlocklistStore.blacklist(token, expiresAt);
    }

    public boolean isTokenBlacklisted(String token) {
        return accessTokenBlocklistStore.isBlacklisted(token);
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenBlacklisted(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
