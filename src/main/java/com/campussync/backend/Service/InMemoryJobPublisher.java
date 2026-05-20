package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Dto.NotificationJobMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class InMemoryJobPublisher implements JobPublisher {

    private final JobRuntimeSwitch jobRuntimeSwitch;
    private final EmailDeliveryService emailDeliveryService;
    private final WebSocketNotificationSender webSocketNotificationSender;
    private final ObjectProvider<RabbitJobPublisher> rabbitJobPublisher;

    public InMemoryJobPublisher(JobRuntimeSwitch jobRuntimeSwitch,
                                EmailDeliveryService emailDeliveryService,
                                WebSocketNotificationSender webSocketNotificationSender,
                                ObjectProvider<RabbitJobPublisher> rabbitJobPublisher) {
        this.jobRuntimeSwitch = jobRuntimeSwitch;
        this.emailDeliveryService = emailDeliveryService;
        this.webSocketNotificationSender = webSocketNotificationSender;
        this.rabbitJobPublisher = rabbitJobPublisher;
    }

    @Override
    public void publishEmail(EmailJobMessage message) {
        if (jobRuntimeSwitch.useRabbit()) {
            rabbitJobPublisher.getObject().publishEmail(message);
            return;
        }
        emailDeliveryService.deliver(message);
    }

    @Override
    public void publishNotification(NotificationJobMessage message) {
        if (jobRuntimeSwitch.useRabbit()) {
            rabbitJobPublisher.getObject().publishNotification(message);
            return;
        }
        webSocketNotificationSender.send(message.notification(), message.userId(), message.email());
    }
}
