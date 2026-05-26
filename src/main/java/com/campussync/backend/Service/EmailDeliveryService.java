package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class EmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM:CampusSync <onboarding@resend.dev>}")
    private String resendFrom;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EmailDeliveryService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void deliver(EmailJobMessage job) {
        try {
            if (resendApiKey != null && !resendApiKey.isBlank()) {
                deliverViaResend(job);
            } else if (mailSender != null) {
                deliverViaSmtp(job);
            } else {
                log.warn("No email provider configured. Simulating email to {}: {}", job.to(), job.subject());
            }
        } catch (Exception e) {
            log.error("Failed to deliver email to {}: {}", job.to(), e.getMessage(), e);
        }
    }

    private void deliverViaResend(EmailJobMessage job) throws Exception {
        String jsonBody = """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "%s",
                  "text": "%s"
                }
                """.formatted(
                escapeJson(resendFrom),
                escapeJson(job.to()),
                escapeJson(job.subject()),
                escapeJson(job.body())
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("Email delivered via Resend to {}", job.to());
        } else {
            log.error("Resend API error (HTTP {}): {}", response.statusCode(), response.body());
            throw new RuntimeException("Resend API returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private void deliverViaSmtp(EmailJobMessage job) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(job.to());
        message.setSubject(job.subject());
        message.setText(job.body());
        mailSender.send(message);
        log.info("Email delivered via SMTP to {}", job.to());
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
