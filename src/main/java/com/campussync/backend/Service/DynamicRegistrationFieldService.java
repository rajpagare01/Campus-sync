package com.campussync.backend.Service;

import com.campussync.backend.Dto.EventRegistrationAnswerRequest;
import com.campussync.backend.Dto.EventRegistrationAnswerResponse;
import com.campussync.backend.Dto.EventRegistrationFieldRequest;
import com.campussync.backend.Dto.EventRegistrationFieldResponse;
import com.campussync.backend.Exception.BadRequestException;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.NotFoundException;
import com.campussync.backend.Model.*;
import com.campussync.backend.Repository.EventRegistrationAnswerRepository;
import com.campussync.backend.Repository.EventRegistrationFieldRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DynamicRegistrationFieldService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final EventRegistrationFieldRepository fieldRepository;
    private final EventRegistrationAnswerRepository answerRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────────────────
    //  FIELD MANAGEMENT (event creator only)
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public List<EventRegistrationFieldResponse> setFields(Long eventId, List<EventRegistrationFieldRequest> requests) {
        User currentUser = getCurrentUser();
        Event event = getOwnedEvent(eventId, currentUser);

        // Delete all existing fields (replace strategy)
        fieldRepository.deleteByEventId(eventId);
        fieldRepository.flush();

        List<EventRegistrationField> fields = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            EventRegistrationFieldRequest req = requests.get(i);
            EventRegistrationField field = EventRegistrationField.builder()
                    .event(event)
                    .label(req.getLabel())
                    .fieldKey(req.getFieldKey())
                    .type(req.getType())
                    .required(req.isRequired())
                    .options(serializeOptions(req.getOptions()))
                    .placeholder(req.getPlaceholder())
                    .displayOrder(req.getDisplayOrder() > 0 ? req.getDisplayOrder() : i)
                    .build();
            fields.add(fieldRepository.save(field));
        }

        return fields.stream().map(this::toFieldResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationFieldResponse> getFields(Long eventId) {
        return fieldRepository.findByEventIdOrderByDisplayOrderAsc(eventId)
                .stream()
                .map(this::toFieldResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ANSWER SUBMISSION (during registration)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validate and save answers for a registration.
     * This is called from RegistrationService after the Registration entity is persisted.
     */
    @Transactional
    public void saveAnswers(Registration registration, List<EventRegistrationAnswerRequest> answerRequests) {
        Long eventId = registration.getEvent().getId();
        List<EventRegistrationField> fields = fieldRepository.findByEventIdOrderByDisplayOrderAsc(eventId);

        if (fields.isEmpty()) {
            // Backward compat: event has no custom fields — skip silently
            return;
        }

        Map<Long, EventRegistrationField> fieldMap = fields.stream()
                .collect(Collectors.toMap(EventRegistrationField::getId, Function.identity()));

        List<EventRegistrationAnswerRequest> safeAnswers =
                (answerRequests == null) ? Collections.emptyList() : answerRequests;

        Map<Long, String> answerMap = safeAnswers.stream()
                .filter(a -> a.getFieldId() != null)
                .collect(Collectors.toMap(EventRegistrationAnswerRequest::getFieldId,
                        a -> a.getAnswerValue() == null ? "" : a.getAnswerValue()));

        // Validate required fields and type constraints
        for (EventRegistrationField field : fields) {
            String value = answerMap.getOrDefault(field.getId(), null);
            if (field.isRequired() && (value == null || value.isBlank())) {
                throw new BadRequestException("Required field missing: " + field.getLabel());
            }
            if (value != null && !value.isBlank()) {
                validateFieldValue(field, value);
            }
        }

        // Persist answers
        for (EventRegistrationAnswerRequest req : safeAnswers) {
            if (req.getFieldId() == null) continue;
            EventRegistrationField field = fieldMap.get(req.getFieldId());
            if (field == null) {
                throw new NotFoundException("Registration field not found: id=" + req.getFieldId());
            }

            EventRegistrationAnswer answer = EventRegistrationAnswer.builder()
                    .registration(registration)
                    .field(field)
                    .fieldKey(field.getFieldKey())
                    .labelSnapshot(field.getLabel())
                    .answerValue(req.getAnswerValue())
                    .build();
            answerRepository.save(answer);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  ANSWER RETRIEVAL
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventRegistrationAnswerResponse> getAnswersForRegistration(Long registrationId) {
        return answerRepository.findByRegistrationId(registrationId)
                .stream()
                .map(this::toAnswerResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<EventRegistrationAnswerResponse>> getAnswersForRegistrations(List<Long> registrationIds) {
        return answerRepository.findByRegistrationIdIn(registrationIds)
                .stream()
                .collect(Collectors.groupingBy(
                        a -> a.getRegistration().getId(),
                        Collectors.mapping(this::toAnswerResponse, Collectors.toList())
                ));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private void validateFieldValue(EventRegistrationField field, String value) {
        switch (field.getType()) {
            case EMAIL -> {
                if (!EMAIL_PATTERN.matcher(value).matches()) {
                    throw new BadRequestException("Invalid email format for field: " + field.getLabel());
                }
            }
            case NUMBER -> {
                if (!NUMBER_PATTERN.matcher(value).matches()) {
                    throw new BadRequestException("Field '" + field.getLabel() + "' must be a number");
                }
            }
            case SELECT -> {
                List<String> options = deserializeOptions(field.getOptions());
                if (!options.isEmpty() && !options.contains(value)) {
                    throw new BadRequestException("Invalid option '" + value + "' for field: " + field.getLabel()
                            + ". Allowed: " + options);
                }
            }
            case MULTI_SELECT -> {
                List<String> options = deserializeOptions(field.getOptions());
                if (!options.isEmpty()) {
                    String[] selected = value.split(",");
                    for (String sel : selected) {
                        String trimmed = sel.trim();
                        if (!options.contains(trimmed)) {
                            throw new BadRequestException("Invalid option '" + trimmed + "' for field: " + field.getLabel()
                                    + ". Allowed: " + options);
                        }
                    }
                }
            }
            default -> {
                // TEXT, TEXTAREA, PHONE, CHECKBOX, DATE — no further server-side validation for MVP
            }
        }
    }

    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid options format");
        }
    }

    private List<String> deserializeOptions(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public EventRegistrationFieldResponse toFieldResponse(EventRegistrationField field) {
        EventRegistrationFieldResponse r = new EventRegistrationFieldResponse();
        r.setId(field.getId());
        r.setEventId(field.getEvent().getId());
        r.setLabel(field.getLabel());
        r.setFieldKey(field.getFieldKey());
        r.setType(field.getType());
        r.setRequired(field.isRequired());
        r.setOptions(deserializeOptions(field.getOptions()));
        r.setPlaceholder(field.getPlaceholder());
        r.setDisplayOrder(field.getDisplayOrder());
        return r;
    }

    private EventRegistrationAnswerResponse toAnswerResponse(EventRegistrationAnswer answer) {
        EventRegistrationAnswerResponse r = new EventRegistrationAnswerResponse();
        r.setFieldId(answer.getField().getId());
        r.setFieldKey(answer.getFieldKey());
        r.setLabel(answer.getLabelSnapshot());
        r.setAnswerValue(answer.getAnswerValue());
        return r;
    }

    private Event getOwnedEvent(Long eventId, User currentUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        boolean isOwner = event.getCreatedBy() != null && event.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("Only the event creator or admin can manage registration fields");
        }
        return event;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
