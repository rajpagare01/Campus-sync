package com.campussync.backend.Dto;

import com.campussync.backend.Model.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private Long actorId;
    private String type;
    private Long relatedId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    
    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.userId = notification.getUserId();
        this.actorId = notification.getActorId();
        this.type = notification.getType().name();
        this.relatedId = notification.getRelatedId();
        this.message = notification.getMessage();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
        this.readAt = notification.getReadAt();
    }
}
