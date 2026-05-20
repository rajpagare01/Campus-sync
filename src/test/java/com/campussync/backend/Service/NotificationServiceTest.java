package com.campussync.backend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.campussync.backend.Model.Notification;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.NotificationRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private NotificationService notificationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notifyFollowBroadcastsToRecipientEmail() {
        User follower = new User();
        follower.setId(1L);
        follower.setName("Follower");
        follower.setEmail("follower@example.com");

        User followed = new User();
        followed.setId(2L);
        followed.setName("Followed");
        followed.setEmail("followed@example.com");

        Notification savedNotification = new Notification();
        savedNotification.setId(10L);
        savedNotification.setUserId(2L);
        savedNotification.setActorId(1L);
        savedNotification.setType(Notification.NotificationType.FOLLOW);
        savedNotification.setRelatedId(1L);
        savedNotification.setMessage("Follower started following you");
        savedNotification.setIsRead(false);
        savedNotification.setCreatedAt(LocalDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(userRepository.findById(2L)).thenReturn(Optional.of(followed));

        notificationService.notifyFollow(follower, followed);

        verify(notificationDispatchService).dispatch(any(), eq(2L), eq("followed@example.com"));
    }

    @Test
    void markAsReadForCurrentUserRejectsForeignNotification() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("owner@example.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsReadForCurrentUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void markAsReadForCurrentUserUpdatesReadStateAndTimestamp() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("owner@example.com");

        Notification notification = new Notification();
        notification.setId(7L);
        notification.setUserId(1L);
        notification.setActorId(2L);
        notification.setType(Notification.NotificationType.LIKE);
        notification.setMessage("Test notification");
        notification.setIsRead(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@example.com", null)
        );

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(notification));

        notificationService.markAsReadForCurrentUser(7L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getIsRead()).isTrue();
        assertThat(captor.getValue().getReadAt()).isNotNull();
    }
}
