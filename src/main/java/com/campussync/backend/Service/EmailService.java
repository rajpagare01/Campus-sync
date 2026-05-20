package com.campussync.backend.Service;

import com.campussync.backend.Dto.EmailJobMessage;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.PaymentOrder;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService{

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter EVENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US);

    private final JobPublisher jobPublisher;

    public EmailService(JobPublisher jobPublisher) {
        this.jobPublisher = jobPublisher;
    }

    public void sendEmail(String to, String subject, String body) {
        jobPublisher.publishEmail(new EmailJobMessage(to, subject, body));
        log.info("Email job queued for {}", to);
    }

    public void sendEventRegistrationConfirmation(User user, Event event, Registration registration) {
        String eventDate = event.getDate() == null
                ? "To be announced"
                : event.getDate().format(EVENT_DATE_FORMAT);
        String venue = isBlank(event.getVenue()) ? "To be announced" : event.getVenue();

        String body = """
                Hello %s,

                Your registration is confirmed.

                Registration ID: %s
                Event: %s
                Date and Time: %s
                Venue: %s
                Status: %s

                Please keep this email for your records.

                Regards,
                CampusSync Team
                """.formatted(
                user.getName(),
                registration.getId(),
                event.getTitle(),
                eventDate,
                venue,
                registration.getStatus()
        );

        sendEmail(user.getEmail(), "Event Registration Confirmed - " + event.getTitle(), body);
    }

    public void sendPaymentReceipt(User user, Event event, PaymentOrder order) {
        String eventDate = event.getDate() == null
                ? "To be announced"
                : event.getDate().format(EVENT_DATE_FORMAT);
        String venue = isBlank(event.getVenue()) ? "To be announced" : event.getVenue();
        String amount = order.getAmountInMinorUnits() == null
                ? "0.00"
                : String.format(Locale.US, "%.2f", order.getAmountInMinorUnits() / 100.0);

        String body = """
                Hello %s,

                Your payment was received successfully.

                Order ID: %s
                Event: %s
                Date and Time: %s
                Venue: %s
                Amount: %s %s
                Payment Status: %s

                Regards,
                CampusSync Team
                """.formatted(
                user.getName(),
                order.getId(),
                event.getTitle(),
                eventDate,
                venue,
                order.getCurrency(),
                amount,
                order.getStatus()
        );

        sendEmail(user.getEmail(), "Payment Receipt - " + event.getTitle(), body);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
