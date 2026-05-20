package com.campussync.backend.Repository;

import com.campussync.backend.Model.Notification;
import com.campussync.backend.Model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("NotificationRepository Integration Tests")
public class NotificationRepositoryTest {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    private User testActor;
    
    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);
        
        testActor = new User();
        testActor.setName("Actor User");
        testActor.setEmail("actor@example.com");
        testActor.setPassword("password");
        testActor = userRepository.save(testActor);
    }
    
    @Test
    @DisplayName("Should save and retrieve notification")
    void testSaveAndFindNotification() {
        // Arrange
        Notification notification = new Notification();
        notification.setUserId(testUser.getId());
        notification.setActorId(testActor.getId());
        notification.setType(Notification.NotificationType.LIKE);
        notification.setRelatedId(1L);
        notification.setMessage("Test message");
        notification.setIsRead(false);
        
        // Act
        Notification saved = notificationRepository.save(notification);
        
        // Assert
        assertNotNull(saved.getId());
        assertEquals(testUser.getId(), saved.getUserId());
        assertEquals(testActor.getId(), saved.getActorId());
        assertEquals(Notification.NotificationType.LIKE, saved.getType());
    }
    
    @Test
    @DisplayName("Should find notifications by user ID ordered by created date")
    void testFindByUserIdOrderByCreatedAtDesc() {
        // Arrange
        Notification notif1 = new Notification();
        notif1.setUserId(testUser.getId());
        notif1.setActorId(testActor.getId());
        notif1.setType(Notification.NotificationType.LIKE);
        notif1.setMessage("First notification");
        notif1.setIsRead(false);
        notif1.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        
        Notification notif2 = new Notification();
        notif2.setUserId(testUser.getId());
        notif2.setActorId(testActor.getId());
        notif2.setType(Notification.NotificationType.COMMENT);
        notif2.setMessage("Second notification");
        notif2.setIsRead(false);
        notif2.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notif1);
        notificationRepository.save(notif2);
        
        // Act
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        
        // Assert
        assertEquals(2, result.size());
        // Most recent should be first
        assertEquals("Second notification", result.get(0).getMessage());
        assertEquals("First notification", result.get(1).getMessage());
    }
    
    @Test
    @DisplayName("Should find unread notifications for user")
    void testFindByUserIdAndIsReadFalse() {
        // Arrange
        Notification readNotif = new Notification();
        readNotif.setUserId(testUser.getId());
        readNotif.setActorId(testActor.getId());
        readNotif.setType(Notification.NotificationType.LIKE);
        readNotif.setMessage("Read notification");
        readNotif.setIsRead(true);
        
        Notification unreadNotif = new Notification();
        unreadNotif.setUserId(testUser.getId());
        unreadNotif.setActorId(testActor.getId());
        unreadNotif.setType(Notification.NotificationType.COMMENT);
        unreadNotif.setMessage("Unread notification");
        unreadNotif.setIsRead(false);
        
        notificationRepository.save(readNotif);
        notificationRepository.save(unreadNotif);
        
        // Act
        List<Notification> result = notificationRepository.findByUserIdAndIsReadFalse(testUser.getId());
        
        // Assert
        assertEquals(1, result.size());
        assertEquals("Unread notification", result.get(0).getMessage());
    }
    
    @Test
    @DisplayName("Should count unread notifications")
    void testCountByUserIdAndIsReadFalse() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            Notification notif = new Notification();
            notif.setUserId(testUser.getId());
            notif.setActorId(testActor.getId());
            notif.setType(Notification.NotificationType.LIKE);
            notif.setMessage("Unread notification " + i);
            notif.setIsRead(false);
            notificationRepository.save(notif);
        }
        
        // Act
        long count = notificationRepository.countByUserIdAndIsReadFalse(testUser.getId());
        
        // Assert
        assertEquals(5, count);
    }
    
    @Test
    @DisplayName("Should mark all notifications as read for user")
    void testMarkAllAsReadForUser() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            Notification notif = new Notification();
            notif.setUserId(testUser.getId());
            notif.setActorId(testActor.getId());
            notif.setType(Notification.NotificationType.LIKE);
            notif.setMessage("Notification " + i);
            notif.setIsRead(false);
            notificationRepository.save(notif);
        }
        
        // Act
        int markedCount = notificationRepository.markAllAsReadForUser(testUser.getId());
        
        // Assert
        assertEquals(3, markedCount);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(testUser.getId());
        assertEquals(0, unreadCount);
    }
    
    @Test
    @DisplayName("Should mark single notification as read")
    void testMarkAsRead() {
        // Arrange
        Notification notif = new Notification();
        notif.setUserId(testUser.getId());
        notif.setActorId(testActor.getId());
        notif.setType(Notification.NotificationType.FOLLOW);
        notif.setMessage("Follow notification");
        notif.setIsRead(false);
        Notification saved = notificationRepository.save(notif);
        
        // Act
        notificationRepository.markAsRead(saved.getId());
        Notification updated = notificationRepository.findById(saved.getId()).get();
        
        // Assert
        assertTrue(updated.getIsRead());
    }
    
    @Test
    @DisplayName("Should find notifications by type")
    void testFindByUserIdAndTypeOrderByCreatedAtDesc() {
        // Arrange
        Notification likeNotif = new Notification();
        likeNotif.setUserId(testUser.getId());
        likeNotif.setActorId(testActor.getId());
        likeNotif.setType(Notification.NotificationType.LIKE);
        likeNotif.setMessage("Like notification");
        
        Notification commentNotif = new Notification();
        commentNotif.setUserId(testUser.getId());
        commentNotif.setActorId(testActor.getId());
        commentNotif.setType(Notification.NotificationType.COMMENT);
        commentNotif.setMessage("Comment notification");
        
        notificationRepository.save(likeNotif);
        notificationRepository.save(commentNotif);
        
        // Act
        List<Notification> result = notificationRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(testUser.getId(), Notification.NotificationType.LIKE);
        
        // Assert
        assertEquals(1, result.size());
        assertEquals(Notification.NotificationType.LIKE, result.get(0).getType());
        assertEquals("Like notification", result.get(0).getMessage());
    }
}
