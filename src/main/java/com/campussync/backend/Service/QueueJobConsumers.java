package com.campussync.backend.Service;

import com.campussync.backend.config.QueueTopologyConfig;
import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Dto.NotificationJobMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.jobs.mode", havingValue = "rabbit")
public class QueueJobConsumers {

    private final EmailDeliveryService emailDeliveryService;
    private final WebSocketNotificationSender webSocketNotificationSender;
    private final JobRuntimeSwitch jobRuntimeSwitch;

    public QueueJobConsumers(EmailDeliveryService emailDeliveryService,
                             WebSocketNotificationSender webSocketNotificationSender,
                             JobRuntimeSwitch jobRuntimeSwitch) {
        this.emailDeliveryService = emailDeliveryService;
        this.webSocketNotificationSender = webSocketNotificationSender;
        this.jobRuntimeSwitch = jobRuntimeSwitch;
    }

    @RabbitListener(queues = QueueTopologyConfig.EMAIL_QUEUE)
    public void consumeEmail(EmailJobMessage message) {
        if (jobRuntimeSwitch.useRabbit()) {
            emailDeliveryService.deliver(message);
        }
    }

    @RabbitListener(queues = QueueTopologyConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(NotificationJobMessage message) {
        if (jobRuntimeSwitch.useRabbit()) {
            webSocketNotificationSender.send(message.notification(), message.userId(), message.email());
        }
    }
}
