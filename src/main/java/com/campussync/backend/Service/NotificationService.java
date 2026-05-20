package com.campussync.backend.Service;

import com.campussync.backend.Dto.NotificationDTO;
import com.campussync.backend.Model.*;
import com.campussync.backend.Repository.NotificationRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatchService notificationDispatchService;
    
    /**
     * Notify when a post receives a like
     */
    public void notifyLike(Post post, User liker) {
        // Bug #3 Fix: Do not notify users when they like their own post
        if (liker.getId().equals(post.getAuthor().getId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(post.getAuthor().getId());
        notification.setActorId(liker.getId());
        notification.setType(Notification.NotificationType.LIKE);
        notification.setRelatedId(post.getId());
        notification.setMessage(liker.getName() + " liked your post");
        
        saveAndBroadcast(notification);
    }
    
    /**
     * Notify when a comment is added to a post
     */
    public void notifyComment(Comment comment) {
        // Bug #4 Fix: Do not notify users when they comment on their own post
        if (comment.getAuthor().getId().equals(comment.getPost().getAuthor().getId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(comment.getPost().getAuthor().getId());
        notification.setActorId(comment.getAuthor().getId());
        notification.setType(Notification.NotificationType.COMMENT);
        notification.setRelatedId(comment.getPost().getId());
        notification.setMessage(comment.getAuthor().getName() + " commented on your post");
        
        saveAndBroadcast(notification);
    }
    
    /**
     * Notify when a user is followed
     */
    public void notifyFollow(User follower, User followedUser) {
        Notification notification = new Notification();
        notification.setUserId(followedUser.getId());
        notification.setActorId(follower.getId());
        notification.setType(Notification.NotificationType.FOLLOW);
        notification.setRelatedId(follower.getId());
        notification.setMessage(follower.getName() + " started following you");
        
        saveAndBroadcast(notification);
    }
    
    /**
     * Notify when an event is updated
     */
    public void notifyEventUpdate(Event event, String updateMessage) {
        if (event.getCreatedBy() == null || event.getCreatedBy().getId() == null) {
            return;
        }

        registrationRepository.findByEventId(event.getId()).stream()
                .filter(Registration::isRegistered)
                .map(registration -> registration.getUser().getId())
                .distinct()
                .forEach(userId -> {
                    Notification notification = new Notification();
                    notification.setUserId(userId);
                    notification.setActorId(event.getCreatedBy().getId());
                    notification.setType(Notification.NotificationType.EVENT_UPDATE);
                    notification.setRelatedId(event.getId());
                    notification.setMessage("Event '" + event.getTitle() + "' was updated: " + updateMessage);
                    saveAndBroadcast(notification);
                });
    }

    public void notifyEventBroadcast(Event event, User participant, String message) {
        User sender = event.getCreatedBy();
        if (sender == null || sender.getId() == null) {
            throw new IllegalArgumentException("Event broadcast sender could not be resolved");
        }
        notifyEventBroadcast(event, participant, sender, message);
    }

    public void notifyEventBroadcast(Event event, User participant, User sender, String message) {
        Notification notification = new Notification();
        notification.setUserId(participant.getId());
        notification.setActorId(sender.getId());
        notification.setType(Notification.NotificationType.EVENT_BROADCAST);
        notification.setRelatedId(event.getId());
        notification.setMessage("Broadcast for '" + event.getTitle() + "': " + message);
        saveAndBroadcast(notification);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  FEATURE 2: Society membership notifications
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Notify society owner that a user has requested to join.
     */
    public void notifySocietyJoinRequest(User societyOwner, User requester, Long societyId, Long requestId) {
        Notification notification = new Notification();
        notification.setUserId(societyOwner.getId());
        notification.setActorId(requester.getId());
        notification.setType(Notification.NotificationType.SOCIETY_JOIN_REQUEST);
        notification.setRelatedId(requestId);
        notification.setMessage(requester.getName() + " has requested to join your society");
        saveAndBroadcast(notification);
    }

    /**
     * Notify the requester that their join request was accepted.
     */
    public void notifySocietyJoinAccepted(User requester, User reviewer, Long societyId) {
        Notification notification = new Notification();
        notification.setUserId(requester.getId());
        notification.setActorId(reviewer.getId());
        notification.setType(Notification.NotificationType.SOCIETY_JOIN_ACCEPTED);
        notification.setRelatedId(societyId);
        notification.setMessage("Your request to join the society has been accepted");
        saveAndBroadcast(notification);
    }

    /**
     * Notify the requester that their join request was rejected.
     */
    public void notifySocietyJoinRejected(User requester, User reviewer, Long societyId, String reason) {
        Notification notification = new Notification();
        notification.setUserId(requester.getId());
        notification.setActorId(reviewer.getId());
        notification.setType(Notification.NotificationType.SOCIETY_JOIN_REJECTED);
        notification.setRelatedId(societyId);
        String msg = "Your request to join the society has been rejected";
        if (reason != null && !reason.isBlank()) {
            msg += ": " + reason;
        }
        notification.setMessage(msg);
        saveAndBroadcast(notification);
    }
    
    /**
     * Get all notifications for a user
     */
    public List<NotificationDTO> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get unread notifications for a user
     */
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream()
                .map(NotificationDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get count of unread notifications
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    public void markAsReadForCurrentUser(Long notificationId) {
        Notification notification = getOwnedNotification(notificationId, getCurrentUserId());
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    public void markAsReadForUser(Long notificationId, Long userId) {
        Notification notification = getOwnedNotification(notificationId, userId);
        notification.markAsRead();
        notificationRepository.save(notification);
    }
    
    /**
     * Mark all notifications as read for a user
     */
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadForUser(userId);
    }

    public int markAllAsReadForCurrentUser() {
        return markAllAsRead(getCurrentUserId());
    }
    
    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    public void deleteNotificationForCurrentUser(Long notificationId) {
        Notification notification = getOwnedNotification(notificationId);
        notificationRepository.delete(notification);
    }

    public List<NotificationDTO> getNotificationsForCurrentUser() {
        return getNotifications(getCurrentUserId());
    }

    public List<NotificationDTO> getUnreadNotificationsForCurrentUser() {
        return getUnreadNotifications(getCurrentUserId());
    }

    public long getUnreadCountForCurrentUser() {
        return getUnreadCount(getCurrentUserId());
    }

    public Long resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }

        return resolveUserId(auth.getName());
    }
    
    /**
     * Save notification and broadcast to user via WebSocket
     */
    private void saveAndBroadcast(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        NotificationDTO dto = new NotificationDTO(saved);
        String email = userRepository.findById(saved.getUserId())
                .map(User::getEmail)
                .orElse(null);
        notificationDispatchService.dispatch(dto, saved.getUserId(), email);
    }

    private Notification getOwnedNotification(Long notificationId) {
        return getOwnedNotification(notificationId, getCurrentUserId());
    }

    private Notification getOwnedNotification(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }
}
