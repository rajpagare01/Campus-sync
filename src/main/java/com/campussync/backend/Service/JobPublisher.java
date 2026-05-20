package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Dto.NotificationJobMessage;

public interface JobPublisher {
    void publishEmail(EmailJobMessage message);
    void publishNotification(NotificationJobMessage message);
}
