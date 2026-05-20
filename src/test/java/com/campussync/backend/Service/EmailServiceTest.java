package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JobPublisher jobPublisher;

    @Test
    void sendEventRegistrationConfirmationBuildsConfirmationEmail() {
        EmailService emailService = new EmailService(jobPublisher);

        User user = new User();
        user.setName("Student User");
        user.setEmail("student@example.com");

        Event event = new Event();
        event.setTitle("Campus Hackathon");
        event.setVenue("Auditorium");
        event.setDate(LocalDateTime.of(2026, 4, 25, 10, 0));

        Registration registration = new Registration();
        registration.setId(99L);
        registration.setStatus(RegistrationStatus.REGISTERED);

        emailService.sendEventRegistrationConfirmation(user, event, registration);

        ArgumentCaptor<EmailJobMessage> messageCaptor = ArgumentCaptor.forClass(EmailJobMessage.class);
        verify(jobPublisher).publishEmail(messageCaptor.capture());

        EmailJobMessage message = messageCaptor.getValue();
        assertThat(message.to()).isEqualTo("student@example.com");
        assertThat(message.subject()).isEqualTo("Event Registration Confirmed - Campus Hackathon");
        assertThat(message.body())
                .contains("Your registration is confirmed.")
                .contains("Registration ID: 99")
                .contains("Event: Campus Hackathon")
                .contains("Date and Time: 25 Apr 2026, 10:00 AM")
                .contains("Venue: Auditorium")
                .contains("Status: REGISTERED");
    }
}
