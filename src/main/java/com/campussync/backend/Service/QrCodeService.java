package com.campussync.backend.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class QrCodeService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key key;

    @PostConstruct
    void init() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateRegistrationQrCode(Long registrationId, Long userId) {
        return Jwts.builder()
                .setSubject("event-check-in")
                .claim("registrationId", registrationId)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .signWith(key)
                .compact();
    }

    public QrPayload parse(String qrCode) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(qrCode)
                .getBody();

        if (!"event-check-in".equals(claims.getSubject())) {
            throw new RuntimeException("Invalid QR code");
        }

        Number registrationId = claims.get("registrationId", Number.class);
        Number userId = claims.get("userId", Number.class);
        if (registrationId == null || userId == null) {
            throw new RuntimeException("Invalid QR code");
        }

        return new QrPayload(registrationId.longValue(), userId.longValue());
    }

    public record QrPayload(Long registrationId, Long userId) {
    }
}
