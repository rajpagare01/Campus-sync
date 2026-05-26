package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailDeliveryService {

    private final JavaMailSender mailSender;

    public EmailDeliveryService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void deliver(EmailJobMessage job) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Simulating email delivery to {}: {}", job.to(), job.subject());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(job.to());
            message.setSubject(job.subject());
            message.setText(job.body());
            mailSender.send(message);
            log.info("Email delivered to {}", job.to());
        } catch (Exception e) {
            log.error("Failed to deliver email to {}: {}", job.to(), e.getMessage(), e);
        }
    }
}

