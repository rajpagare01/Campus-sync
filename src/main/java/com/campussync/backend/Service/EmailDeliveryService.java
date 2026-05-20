package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailDeliveryService {

    private final JavaMailSender mailSender;

    public EmailDeliveryService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void deliver(EmailJobMessage job) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(job.to());
        message.setSubject(job.subject());
        message.setText(job.body());
        mailSender.send(message);
        log.info("Email delivered to {}", job.to());
    }
}
