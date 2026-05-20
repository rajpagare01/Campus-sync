package com.campussync.backend.Service;

import com.campussync.backend.Dto.FeedbackRequest;
import com.campussync.backend.Dto.FeedbackResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.NotFoundException;
import com.campussync.backend.Model.AttendanceStatus;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Feedback;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.FeedbackRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse submitFeedback(Long eventId, FeedbackRequest request) {
        User user = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        Registration registration = registrationRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new ForbiddenOperationException("Only event participants can submit feedback"));

        if (registration.getAttendanceStatus() != AttendanceStatus.CHECKED_IN) {
            throw new ForbiddenOperationException("Feedback is allowed only after attendance is marked");
        }

        Feedback feedback = feedbackRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElseGet(Feedback::new);

        feedback.setEvent(event);
        feedback.setUser(user);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        return toResponse(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<FeedbackResponse> getFeedback(Long eventId, int page, int size) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbackPage = feedbackRepository.findByEventIdOrderByCreatedAtDesc(eventId, pageable);
        Page<FeedbackResponse> mapped = feedbackPage.map(this::toResponse);

        return new PaginatedResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages(),
                mapped.isFirst(),
                mapped.isLast(),
                mapped.isEmpty()
        );
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        response.setEventId(feedback.getEvent().getId());
        response.setUserId(feedback.getUser().getId());
        response.setUserName(feedback.getUser().getName());
        response.setRating(feedback.getRating());
        response.setComment(feedback.getComment());
        response.setCreatedAt(feedback.getCreatedAt());
        response.setUpdatedAt(feedback.getUpdatedAt());
        return response;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
