package com.campussync.backend.Service;

import com.campussync.backend.config.QueueTopologyConfig;
import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Dto.NotificationJobMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.jobs.mode", havingValue = "rabbit")
public class RabbitJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitJobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishEmail(EmailJobMessage message) {
        rabbitTemplate.convertAndSend(QueueTopologyConfig.JOBS_EXCHANGE, "email", message);
    }

    public void publishNotification(NotificationJobMessage message) {
        rabbitTemplate.convertAndSend(QueueTopologyConfig.JOBS_EXCHANGE, "notification", message);
    }
}
