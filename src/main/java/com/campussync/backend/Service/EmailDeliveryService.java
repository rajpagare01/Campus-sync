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

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${BREVO_SENDER_EMAIL:rajpagare305@gmail.com}")
    private String brevoSenderEmail;

    @Value("${BREVO_SENDER_NAME:CampusSync}")
    private String brevoSenderName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EmailDeliveryService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void deliver(EmailJobMessage job) {
        try {
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                deliverViaBrevo(job);
            } else if (mailSender != null) {
                deliverViaSmtp(job);
            } else {
                log.warn("No email provider configured. Simulating email to {}: {}", job.to(), job.subject());
            }
        } catch (Exception e) {
            log.error("Failed to deliver email to {}: {}", job.to(), e.getMessage(), e);
        }
    }

    private void deliverViaBrevo(EmailJobMessage job) throws Exception {
        String jsonBody = """
                {
                  "sender": {
                    "name": "%s",
                    "email": "%s"
                  },
                  "to": [
                    {
                      "email": "%s"
                    }
                  ],
                  "subject": "%s",
                  "textContent": "%s"
                }
                """.formatted(
                escapeJson(brevoSenderName),
                escapeJson(brevoSenderEmail),
                escapeJson(job.to()),
                escapeJson(job.subject()),
                escapeJson(job.body())
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", brevoApiKey)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("Email delivered via Brevo to {}", job.to());
        } else {
            log.error("Brevo API error (HTTP {}): {}", response.statusCode(), response.body());
            throw new RuntimeException("Brevo API returned HTTP " + response.statusCode() + ": " + response.body());
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
