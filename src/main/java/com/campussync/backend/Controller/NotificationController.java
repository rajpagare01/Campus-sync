package com.campussync.backend.Controller;

import com.campussync.backend.Dto.NotificationDTO;
import com.campussync.backend.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * GET /api/notifications - Get all notifications for current user
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getNotificationsForCurrentUser());
    }
    
    /**
     * GET /api/notifications/unread - Get unread notifications count
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        long count = notificationService.getUnreadCountForCurrentUser();
        List<NotificationDTO> unreadNotifications = notificationService.getUnreadNotificationsForCurrentUser();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("notifications", unreadNotifications);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/notifications/{id}/read - Mark notification as read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsReadForCurrentUser(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * POST /api/notifications/read-all - Mark all notifications as read
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        int count = notificationService.markAllAsReadForCurrentUser();
        
        Map<String, Object> response = new HashMap<>();
        response.put("markedAsRead", count);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/notifications/{id} - Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotificationForCurrentUser(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * WebSocket: Subscribe to user-specific notifications
     * Message format: { "action": "SUBSCRIBE" }
     */
    @MessageMapping("/notifications/subscribe")
    @SendToUser("/queue/notifications")
    public NotificationDTO subscribeToNotifications(@Payload Map<String, String> message, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return websocketError("Please reconnect to notifications with a valid login token.");
        }

        Long userId = notificationService.resolveUserId(authentication.getName());
        // Confirmation message
        NotificationDTO confirmation = new NotificationDTO();
        confirmation.setMessage("Connected to notification service. User ID: " + userId);
        return confirmation;
    }
    
    /**
     * WebSocket: Acknowledge notification read
     * Message format: { "notificationId": 123 }
     */
    @MessageMapping("/notifications/ack")
    @SendToUser("/queue/notifications")
    public Map<String, Object> acknowledgeNotification(@Payload Map<String, Long> message, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "unauthorized");
            response.put("message", "Please reconnect to notifications with a valid login token.");
            return response;
        }

        Long notificationId = message.get("notificationId");
        Long userId = notificationService.resolveUserId(authentication.getName());
        notificationService.markAsReadForUser(notificationId, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("notificationId", notificationId);
        response.put("message", "Notification marked as read");
        
        return response;
    }

    private NotificationDTO websocketError(String message) {
        NotificationDTO error = new NotificationDTO();
        error.setMessage(message);
        return error;
    }
    
}
