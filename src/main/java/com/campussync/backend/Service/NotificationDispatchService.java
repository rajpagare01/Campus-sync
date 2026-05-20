package com.campussync.backend.Service;

import com.campussync.backend.Dto.NotificationJobMessage;
import com.campussync.backend.Dto.NotificationDTO;
import org.springframework.stereotype.Service;

@Service
public class NotificationDispatchService {

    private final JobPublisher jobPublisher;

    public NotificationDispatchService(JobPublisher jobPublisher) {
        this.jobPublisher = jobPublisher;
    }

    public void dispatch(NotificationDTO dto, Long userId, String email) {
        try {
            jobPublisher.publishNotification(new NotificationJobMessage(dto, userId, email));
        } catch (Exception e) {
            // Log the error but do not throw it, preventing transaction rollbacks if Redis is down
            System.err.println("[NotificationDispatchService] Failed to publish notification job: " + e.getMessage());
        }
    }
}
