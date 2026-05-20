package com.campussync.backend.Service;

import com.campussync.backend.Dto.FeedbackRequest;
import com.campussync.backend.Dto.FeedbackResponse;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Model.AttendanceStatus;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Feedback;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.FeedbackRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitFeedbackRejectsUserWithoutAttendance() {
        User user = attendeeUser();
        Event event = event();

        Registration registration = new Registration();
        registration.setId(11L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setAttended(false);
        registration.setAttendanceStatus(AttendanceStatus.NOT_CHECKED_IN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserIdAndEventId(2L, 12L)).thenReturn(Optional.of(registration));

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(5);
        request.setComment("Great event");

        assertThatThrownBy(() -> feedbackService.submitFeedback(12L, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Feedback is allowed only after attendance is marked");
    }

    @Test
    void submitFeedbackPersistsFeedbackForAttendee() {
        User user = attendeeUser();
        Event event = event();

        Registration registration = new Registration();
        registration.setId(11L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setAttended(true);
        registration.setAttendanceStatus(AttendanceStatus.CHECKED_IN);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null)
        );

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserIdAndEventId(2L, 12L)).thenReturn(Optional.of(registration));
        when(feedbackRepository.findByEventIdAndUserId(12L, 2L)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(7L);
            feedback.setCreatedAt(LocalDateTime.of(2026, 5, 3, 14, 0));
            feedback.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 14, 0));
            return feedback;
        });

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(5);
        request.setComment("Great event");

        FeedbackResponse response = feedbackService.submitFeedback(12L, request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getEventId()).isEqualTo(12L);
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Great event");
    }

    private User attendeeUser() {
        User user = new User();
        user.setId(2L);
        user.setEmail("student@example.com");
        user.setName("Student User");
        user.setRole(Role.STUDENT);
        return user;
    }

    private Event event() {
        Event event = new Event();
        event.setId(12L);
        event.setTitle("CampusSync Summit");
        return event;
    }
}
