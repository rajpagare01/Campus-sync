package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Dto.NotificationJobMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.jobs.mode", havingValue = "sync", matchIfMissing = true)
public class SyncJobPublisher implements JobPublisher {

    private final EmailDeliveryService emailDeliveryService;
    private final WebSocketNotificationSender webSocketNotificationSender;

    public SyncJobPublisher(EmailDeliveryService emailDeliveryService, WebSocketNotificationSender webSocketNotificationSender) {
        this.emailDeliveryService = emailDeliveryService;
        this.webSocketNotificationSender = webSocketNotificationSender;
    }

    @Override
    public void publishEmail(EmailJobMessage message) {
        emailDeliveryService.deliver(message);
    }

    @Override
    public void publishNotification(NotificationJobMessage message) {
        webSocketNotificationSender.send(message.notification(), message.userId(), message.email());
    }
}
