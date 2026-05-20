package com.campussync.backend.Service;

import com.campussync.backend.Dto.NotificationDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationSender {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationSender(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void send(NotificationDTO dto, Long userId, String email) {
        if (email != null && !email.isBlank()) {
            messagingTemplate.convertAndSendToUser(email, "/queue/notifications", dto);
        }
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, dto);
    }
}
