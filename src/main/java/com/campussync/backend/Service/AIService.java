package com.campussync.backend.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campussync.backend.Dto.AiChatRequest;
import com.campussync.backend.Dto.AiChatResponse;
import com.campussync.backend.Dto.AiChatSource;
import com.campussync.backend.Dto.AiConversationDetailResponse;
import com.campussync.backend.Dto.AiConversationMessage;
import com.campussync.backend.Dto.AiConversationSummary;
import com.campussync.backend.Exception.AiProviderException;
import com.campussync.backend.Model.AiConversation;
import com.campussync.backend.Model.AiMessage;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.AiConversationRepository;
import com.campussync.backend.Repository.AiMessageRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AIService {

    private static final int MAX_CONTEXT_EVENTS = 6;
    private static final int MAX_PREVIEW_LENGTH = 120;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiBaseUrl;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${ai.fail-fast-on-provider-error:false}")
    private boolean failFastOnProviderError;

    @Autowired
    public AIService(AiConversationRepository conversationRepository,
                     AiMessageRepository messageRepository,
                     UserRepository userRepository,
                     EventRepository eventRepository,
                     RegistrationRepository registrationRepository) {
        this(
                conversationRepository,
                messageRepository,
                userRepository,
                eventRepository,
                registrationRepository,
                new RestTemplate(),
                WebClient.builder().build(),
                new ObjectMapper()
        );
    }

    AIService(AiConversationRepository conversationRepository,
              AiMessageRepository messageRepository,
              UserRepository userRepository,
              EventRepository eventRepository,
              RegistrationRepository registrationRepository,
              RestTemplate restTemplate,
              WebClient webClient,
              ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.restTemplate = restTemplate;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = "aiProvider")
    @Retry(name = "aiProvider")
    public String generate(String prompt) {
        String userPrompt = prompt == null ? "" : prompt.trim();
        if (userPrompt.isBlank()) {
            return "Ask me about CampusSync events, registrations, profiles, posts, or anything else you need help with.";
        }

        if (!isProviderConfigured()) {
            if (failFastOnProviderError) {
                throw providerException(new IllegalStateException("Gemini API key is not configured"));
            }
            return "I can still help with CampusSync tasks, but Gemini is not configured yet. Add GEMINI_API_KEY to enable AI-generated answers.";
        }

        try {
            String answer = callGemini(buildPublicPrompt(userPrompt));
            if (answer == null || answer.isBlank()) {
                return handlePublicProviderFailure(new IllegalStateException("Gemini returned an empty response"));
            }
            return answer;
        } catch (RestClientException | IllegalStateException ex) {
            return handlePublicProviderFailure(ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AiConversationSummary> getConversations() {
        User user = getCurrentUser();
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(this::toConversationSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiConversationDetailResponse getConversation(Long conversationId) {
        User user = getCurrentUser();
        AiConversation conversation = getOwnedConversation(conversationId, user);

        AiConversationDetailResponse response = new AiConversationDetailResponse();
        response.setConversationId(conversation.getId());
        response.setTitle(conversation.getTitle());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());
        response.setMessages(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(this::toConversationMessage)
                .toList());
        return response;
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        User user = getCurrentUser();
        AiConversation conversation = getOwnedConversation(conversationId, user);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    @Transactional
    @CircuitBreaker(name = "aiProvider")
    @Retry(name = "aiProvider")
    public AiChatResponse chat(AiChatRequest request) {
        User user = getCurrentUser();
        AiConversation conversation = resolveConversation(request, user);
        ChatContext context = buildContext(user);
        List<AiMessage> history = getRecentHistory(conversation.getId());
        String userMessage = request.getMessage().trim();

        saveMessage(conversation, user, AiMessage.Role.USER, userMessage);

        boolean fallback = false;
        String answer;
        Event focusedEvent = findFocusedEvent(userMessage, history, context);

        if (!isProviderConfigured()) {
            if (failFastOnProviderError) {
                throw providerException(new IllegalStateException("Gemini API key is not configured"));
            }
            fallback = true;
            answer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
        } else {
            String prompt = buildPrompt(user, userMessage, context, history, focusedEvent);
            try {
                answer = callGemini(prompt);
                if (answer == null || answer.isBlank()) {
                    handleChatProviderFailure(new IllegalStateException("Gemini returned an empty response"));
                    fallback = true;
                    answer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
                }
            } catch (RestClientException | IllegalStateException ex) {
                handleChatProviderFailure(ex);
                fallback = true;
                answer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
            }
        }

        saveMessage(conversation, user, AiMessage.Role.ASSISTANT, answer);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversation.getId());
        response.setAnswer(answer);
        response.setFallback(fallback);
        response.setCreatedAt(LocalDateTime.now());
        response.setSources(buildSources(context));
        return response;
    }

    @Transactional
    @CircuitBreaker(name = "aiProvider")
    @Retry(name = "aiProvider")
    public AiChatResponse regenerate(Long conversationId) {
        User user = getCurrentUser();
        AiConversation conversation = getOwnedConversation(conversationId, user);

        List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (history.isEmpty()) {
            throw new IllegalArgumentException("No messages to regenerate");
        }

        AiMessage lastMessage = history.get(history.size() - 1);
        String userMessage;

        if (lastMessage.getRole() == AiMessage.Role.ASSISTANT) {
            messageRepository.delete(lastMessage);
            history.remove(history.size() - 1);
            if (history.isEmpty()) {
                throw new IllegalArgumentException("No user message to regenerate from");
            }
            AiMessage lastUserMessage = history.get(history.size() - 1);
            userMessage = lastUserMessage.getContent();
        } else {
            userMessage = lastMessage.getContent();
        }

        ChatContext context = buildContext(user);
        
        // Temporarily remove the last user message from history for prompt building
        List<AiMessage> historyForPrompt = new ArrayList<>(history);
        if (!historyForPrompt.isEmpty() && historyForPrompt.get(historyForPrompt.size() - 1).getRole() == AiMessage.Role.USER) {
            historyForPrompt.remove(historyForPrompt.size() - 1);
        }

        boolean fallback = false;
        String answer;
        Event focusedEvent = findFocusedEvent(userMessage, historyForPrompt, context);

        if (!isProviderConfigured()) {
            if (failFastOnProviderError) {
                throw providerException(new IllegalStateException("Gemini API key is not configured"));
            }
            fallback = true;
            answer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
        } else {
            String prompt = buildPrompt(user, userMessage, context, historyForPrompt, focusedEvent);
            try {
                answer = callGemini(prompt);
                if (answer == null || answer.isBlank()) {
                    handleChatProviderFailure(new IllegalStateException("Gemini returned an empty response"));
                    fallback = true;
                    answer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
                }
            } catch (RestClientException | IllegalStateException ex) {
                handleChatProviderFailure(ex);
                fallback = true;
                answer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
            }
        }

        saveMessage(conversation, user, AiMessage.Role.ASSISTANT, answer);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversation.getId());
        response.setAnswer(answer);
        response.setFallback(fallback);
        response.setCreatedAt(LocalDateTime.now());
        response.setSources(buildSources(context));
        return response;
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(AiChatRequest request) {
        User user = getCurrentUser();
        AiConversation conversation = resolveConversation(request, user);
        ChatContext context = buildContext(user);
        List<AiMessage> history = getRecentHistory(conversation.getId());
        String userMessage = request.getMessage().trim();

        saveMessage(conversation, user, AiMessage.Role.USER, userMessage);

        Event focusedEvent = findFocusedEvent(userMessage, history, context);
        String prompt = buildPrompt(user, userMessage, context, history, focusedEvent);

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(60000L);

        new Thread(() -> {
            try {
                boolean fallback = false;
                String fullAnswer;

                if (!isProviderConfigured()) {
                    fallback = true;
                    fullAnswer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(fullAnswer));
                } else {
                    try {
                        fullAnswer = streamGemini(prompt, emitter);
                        if (fullAnswer == null || fullAnswer.isBlank()) {
                            fallback = true;
                            fullAnswer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(fullAnswer));
                        }
                    } catch (Exception ex) {
                        handleChatProviderFailure(ex);
                        fallback = true;
                        fullAnswer = buildContextualGuidanceAnswer(user, userMessage, context, focusedEvent);
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(fullAnswer));
                    }
                }

                saveMessage(conversation, user, AiMessage.Role.ASSISTANT, fullAnswer);
                conversation.setUpdatedAt(LocalDateTime.now());
                conversationRepository.save(conversation);

                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("DONE").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private String handlePublicProviderFailure(Exception ex) {
        if (failFastOnProviderError) {
            throw providerException(ex);
        }
        return "I am having trouble reaching the AI engine right now. You can still ask about CampusSync events, registrations, profiles, or posts once it is available.";
    }

    private void handleChatProviderFailure(Exception ex) {
        if (failFastOnProviderError) {
            throw providerException(ex);
        }
    }

    private AiProviderException providerException(Exception ex) {
        return new AiProviderException("Gemini API failed: " + safeErrorMessage(ex), ex);
    }

    private AiConversationSummary toConversationSummary(AiConversation conversation) {
        AiConversationSummary summary = new AiConversationSummary();
        summary.setConversationId(conversation.getId());
        summary.setTitle(conversation.getTitle());
        summary.setCreatedAt(conversation.getCreatedAt());
        summary.setUpdatedAt(conversation.getUpdatedAt());
        summary.setMessageCount(messageRepository.countByConversationId(conversation.getId()));
        summary.setLastMessagePreview(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(AiMessage::getContent)
                .map(content -> truncate(content, MAX_PREVIEW_LENGTH))
                .orElse(null));
        return summary;
    }

    private AiConversationMessage toConversationMessage(AiMessage message) {
        return new AiConversationMessage(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private boolean isProviderConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private String safeErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)(x-goog-api-key=|key=)[^\\s&]+", "$1[hidden]");
    }

    private AiConversation resolveConversation(AiChatRequest request, User user) {
        if (request.getConversationId() != null) {
            return getOwnedConversation(request.getConversationId(), user);
        }

        AiConversation conversation = new AiConversation();
        conversation.setUser(user);
        conversation.setTitle(toConversationTitle(request.getMessage()));
        return conversationRepository.save(conversation);
    }

    private AiConversation getOwnedConversation(Long conversationId, User user) {
        return conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    private String toConversationTitle(String message) {
        String normalized = message == null ? "New chat" : message.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "New chat";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private void saveMessage(AiConversation conversation, User user, AiMessage.Role role, String content) {
        AiMessage message = new AiMessage();
        message.setConversation(conversation);
        message.setUser(user);
        message.setRole(role);
        message.setContent(content);
        messageRepository.save(message);
    }

    private List<AiMessage> getRecentHistory(Long conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        List<AiMessage> messages = new ArrayList<>(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversationId));
        messages.sort(Comparator.comparing(AiMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return messages;
    }

    private ChatContext buildContext(User user) {
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcomingEvents = eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED).stream()
                .filter(event -> event.getDate() != null && event.getDate().isAfter(now))
                .sorted(Comparator.comparing(Event::getDate))
                .limit(MAX_CONTEXT_EVENTS)
                .toList();

        List<Registration> registrations = registrationRepository.findByUserId(user.getId()).stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.REGISTERED)
                .filter(registration -> registration.getEvent() != null)
                .sorted(Comparator.comparing(
                        registration -> registration.getEvent().getDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .limit(MAX_CONTEXT_EVENTS)
                .toList();

        return new ChatContext(upcomingEvents, registrations);
    }

    private String buildPrompt(User user, String message, ChatContext context, List<AiMessage> history, Event focusedEvent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are CampusSync AI Assistant, a smart and conversational chatbot inside a campus social and event platform.\n");
        prompt.append("Behavior rules:\n");
        prompt.append("- Talk like a normal human assistant: friendly, natural, and not robotic.\n");
        prompt.append("- Do not use predefined, repetitive, or template-style responses.\n");
        prompt.append("- Always infer the user's intent, even when the message is casual, short, or unclear.\n");
        prompt.append("- Support both casual conversation and CampusSync tasks.\n");
        prompt.append("- Use the supplied CampusSync context for events, registrations, user info, and role-aware suggestions.\n");
        prompt.append("- If the requested CampusSync data is present, answer with the real data.\n");
        prompt.append("- If the data is missing, guide the user clearly without inventing facts and without saying fallback/offline.\n");
        prompt.append("- Maintain context from recent messages.\n");
        prompt.append("- If the user asks to write a description, caption, or announcement, use the last mentioned event when available.\n");
        prompt.append("- If the question is unrelated to CampusSync, answer normally and gently guide back to CampusSync if useful.\n");
        prompt.append("- Keep answers concise but helpful. Longer responses are fine when writing descriptions or content.\n\n");

        prompt.append("Current user:\n");
        prompt.append("- Name: ").append(nullSafe(user.getName(), "Campus member")).append("\n");
        prompt.append("- Role: ").append(user.getRole()).append("\n");
        prompt.append("- Role guidance: ").append(roleGuidance(user.getRole())).append("\n\n");
        prompt.append("Current time: ").append(DATE_FORMATTER.format(LocalDateTime.now())).append("\n\n");

        prompt.append("Upcoming published events:\n");
        if (context.upcomingEvents().isEmpty()) {
            prompt.append("- None available in context.\n");
        } else {
            context.upcomingEvents().forEach(event -> prompt.append("- ")
                    .append(eventDetailLine(event, false))
                    .append("\n"));
        }

        prompt.append("\nCurrent user registrations:\n");
        if (context.registrations().isEmpty()) {
            prompt.append("- None.\n");
        } else {
            context.registrations().forEach(registration -> prompt.append("- ")
                    .append(eventDetailLine(registration.getEvent(), true))
                    .append("\n"));
        }

        prompt.append("\nEvent currently in focus:\n");
        if (focusedEvent == null) {
            prompt.append("- None detected. If the user asks for generated content without naming an event, ask which event they mean.\n");
        } else {
            boolean registered = context.registrations().stream()
                    .map(Registration::getEvent)
                    .filter(Objects::nonNull)
                    .anyMatch(event -> Objects.equals(event.getId(), focusedEvent.getId()));
            prompt.append("- ").append(eventDetailLine(focusedEvent, registered)).append("\n");
        }

        if (!history.isEmpty()) {
            prompt.append("\nRecent conversation:\n");
            history.forEach(item -> prompt.append(item.getRole().name())
                    .append(": ")
                    .append(item.getContent())
                    .append("\n"));
        }

        prompt.append("\nUser message:\n");
        prompt.append(message).append("\n\n");
        prompt.append("Assistant answer:");
        return prompt.toString();
    }

    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                geminiGenerateContentUrl(),
                new HttpEntity<>(request, headers),
                Map.class
        );

        return extractGeminiText(response.getBody());
    }

    private String streamGemini(String prompt,
                                org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        StringBuilder fullAnswer = new StringBuilder();

        webClient.post()
                .uri(geminiStreamUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", geminiApiKey)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> {
                    String text = extractGeminiChunk(chunk);
                    if (text != null && !text.isBlank()) {
                        try {
                            fullAnswer.append(text);
                            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(text));
                        } catch (Exception sendException) {
                            throw new IllegalStateException("Failed to push SSE chunk", sendException);
                        }
                    }
                })
                .blockLast();

        return fullAnswer.toString().trim();
    }

    private String geminiGenerateContentUrl() {
        String baseUrl = geminiApiBaseUrl.endsWith("/")
                ? geminiApiBaseUrl.substring(0, geminiApiBaseUrl.length() - 1)
                : geminiApiBaseUrl;
        return baseUrl + "/" + geminiModel + ":generateContent";
    }

    private String geminiStreamUrl() {
        String baseUrl = geminiApiBaseUrl.endsWith("/")
                ? geminiApiBaseUrl.substring(0, geminiApiBaseUrl.length() - 1)
                : geminiApiBaseUrl;
        return baseUrl + "/" + geminiModel + ":streamGenerateContent?alt=sse";
    }

    private String extractGeminiText(Map body) {
        if (body == null) {
            throw new IllegalStateException("Gemini response body was empty");
        }

        Object candidatesValue = body.get("candidates");
        if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini response did not contain candidates");
        }

        StringBuilder text = new StringBuilder();
        for (Object candidateValue : candidates) {
            if (!(candidateValue instanceof Map<?, ?> candidate)) {
                continue;
            }
            Object contentValue = candidate.get("content");
            if (!(contentValue instanceof Map<?, ?> content)) {
                continue;
            }
            Object partsValue = content.get("parts");
            if (!(partsValue instanceof List<?> parts)) {
                continue;
            }
            for (Object partValue : parts) {
                if (partValue instanceof Map<?, ?> part) {
                    Object textValue = part.get("text");
                    if (textValue != null) {
                        if (text.length() > 0) {
                            text.append("\n");
                        }
                        text.append(textValue);
                    }
                }
            }
        }

        String answer = text.toString().trim();
        if (answer.isBlank()) {
            throw new IllegalStateException("Gemini response did not contain text");
        }
        return answer;
    }

    private String extractGeminiChunk(String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return null;
        }

        String normalized = chunk.replaceFirst("^data:\\s*", "").trim();
        if (normalized.isBlank() || "[DONE]".equals(normalized)) {
            return null;
        }

        try {
            Map<String, Object> body = objectMapper.readValue(normalized, new TypeReference<>() {});
            return extractGeminiText(body);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildPublicPrompt(String message) {
        return """
                You are CampusSync AI Assistant, a smart and conversational chatbot.

                Behavior:
                - Talk naturally like a helpful human assistant.
                - Do not use predefined, repetitive, or template-style responses.
                - Support casual conversation and CampusSync tasks.
                - If CampusSync-specific account data is needed but not provided, explain that the user should sign in and use chat for personalized help.
                - Keep the answer concise.

                User message:
                %s

                Assistant answer:
                """.formatted(message);
    }

    private String buildContextualGuidanceAnswer(User user, String message, ChatContext context, Event focusedEvent) {
        String normalized = normalizeForIntent(message);

        if (wantsDescription(normalized)) {
            if (focusedEvent != null) {
                return buildEventDescription(focusedEvent);
            }
            return "I can write that. Which event should I base the description on?";
        }

        if (isGreeting(normalized)) {
            return "I am doing well. How can I help you today: events, registrations, your profile, or something in the feed?";
        }

        if (wantsRegistrations(normalized)) {
            if (context.registrations().isEmpty()) {
                return "You are not registered for any events right now. Want to look at the upcoming events and pick one?";
            }
            String events = context.registrations().stream()
                    .map(registration -> eventLine(registration.getEvent(), true))
                    .collect(Collectors.joining("\n- ", "- ", ""));
            return "You are registered for:\n" + events;
        }

        if (wantsUpcomingEvents(normalized)) {
            if (context.upcomingEvents().isEmpty()) {
                return "I do not see any upcoming published events in the current data. Try the Events page or search by title, venue, or event type.";
            }
            String events = context.upcomingEvents().stream()
                    .map(event -> eventLine(event, false))
                    .collect(Collectors.joining("\n- ", "- ", ""));
            return "I found these upcoming events:\n" + events;
        }

        if (wantsCancelRegistration(normalized)) {
            return "Open the event you registered for, then use the cancel or unregister option. If it is not visible, check that you are signed in with the same account used for registration.";
        }

        if (wantsCreateEvent(normalized)) {
            if (user.getRole() == Role.STUDENT) {
                return "Students can discover and register for events. To create one, use a society or department account, or ask an admin to publish it.";
            }
            return "Open Events, choose Create Event, add the title, description, date, venue, type, capacity, and pricing details, then publish when it is ready.";
        }

        if (wantsCreatePost(normalized)) {
            return "Open the Feed or Posts section, choose Create Post, add your text or media, and publish it. If it is event-related, include the event name, date, venue, and a clear call to action.";
        }

        if (wantsProfileHelp(normalized)) {
            return "Open Profile, choose Edit Profile, update the details you want to change, and save. You can usually update basics like name, bio, department, avatar, and contact details.";
        }

        if (wantsAnalytics(normalized)) {
            if (user.getRole() == Role.ADMIN) {
                return "As an admin, check the Dashboard for platform activity, engagement trends, registrations, and moderation signals.";
            }
            if (user.getRole() == Role.SOCIETY || user.getRole() == Role.DEPARTMENT) {
                return "Open one of your events and go to Analytics to review registrations, engagement, attendance signals, and overall event performance.";
            }
            return "Analytics are mainly for admins, societies, and departments. As a student, your profile and registrations are the best places to track your own activity.";
        }

        if (context.upcomingEvents().isEmpty()) {
            return "I can help with that, but I do not have enough event data in the current context. You can ask about events, registrations, profiles, posts, or role-based suggestions.";
        }

        Event nextEvent = context.upcomingEvents().get(0);
        return "I can help with that. I also see " + nullSafe(nextEvent.getTitle(), "an upcoming event")
                + " coming up at " + nullSafe(nextEvent.getVenue(), "a venue to be announced")
                + ". Ask me for details, registration help, or a post/description for it.";
    }

    private List<AiChatSource> buildSources(ChatContext context) {
        Set<Long> registeredEventIds = context.registrations().stream()
                .map(Registration::getEvent)
                .filter(Objects::nonNull)
                .map(Event::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, AiChatSource> sources = new LinkedHashMap<>();
        context.upcomingEvents().forEach(event -> addSource(sources, event, registeredEventIds.contains(event.getId())));
        context.registrations().forEach(registration -> addSource(sources, registration.getEvent(), true));
        return new ArrayList<>(sources.values());
    }

    private void addSource(Map<Long, AiChatSource> sources, Event event, boolean registered) {
        if (event == null || event.getId() == null || sources.containsKey(event.getId())) {
            return;
        }

        sources.put(event.getId(), new AiChatSource(
                "EVENT",
                event.getId(),
                nullSafe(event.getTitle(), "Campus event"),
                event.getType() == null ? "Event" : event.getType().name(),
                event.getVenue(),
                event.getDate(),
                registered
        ));
    }

    private Event findFocusedEvent(String message, List<AiMessage> history, ChatContext context) {
        List<Event> events = contextEvents(context);
        Event currentMessageMatch = findMentionedEvent(message, events);
        if (currentMessageMatch != null) {
            return currentMessageMatch;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            Event historyMatch = findMentionedEvent(history.get(i).getContent(), events);
            if (historyMatch != null) {
                return historyMatch;
            }
        }

        return events.size() == 1 ? events.get(0) : null;
    }

    private List<Event> contextEvents(ChatContext context) {
        Map<Long, Event> events = new LinkedHashMap<>();
        context.upcomingEvents().stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getId() != null)
                .forEach(event -> events.putIfAbsent(event.getId(), event));
        context.registrations().stream()
                .map(Registration::getEvent)
                .filter(Objects::nonNull)
                .filter(event -> event.getId() != null)
                .forEach(event -> events.putIfAbsent(event.getId(), event));
        return new ArrayList<>(events.values());
    }

    private Event findMentionedEvent(String text, List<Event> events) {
        String normalized = normalizeForIntent(text);
        if (normalized.isBlank()) {
            return null;
        }

        for (Event event : events) {
            String title = normalizeForIntent(event.getTitle());
            if (!title.isBlank() && normalized.contains(title)) {
                return event;
            }
            if (event.getId() != null && containsWholeNumber(normalized, event.getId())) {
                return event;
            }
        }

        for (Event event : events) {
            String title = normalizeForIntent(event.getTitle());
            for (String token : title.split(" ")) {
                if (token.length() >= 5 && normalized.contains(token)) {
                    return event;
                }
            }
        }

        return null;
    }

    private boolean containsWholeNumber(String normalized, Long number) {
        String needle = String.valueOf(number);
        String[] tokens = normalized.split(" ");
        for (String token : tokens) {
            if (needle.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private String eventLine(Event event, boolean registered) {
        if (event == null) {
            return "Unknown event";
        }
        String date = event.getDate() == null ? "date TBA" : DATE_FORMATTER.format(event.getDate());
        String price = Boolean.TRUE.equals(event.getPaid()) ? "paid, price " + event.getPrice() : "free";
        return "[" + event.getId() + "] "
                + nullSafe(event.getTitle(), "Campus event")
                + " | " + date
                + " | " + nullSafe(event.getVenue(), "venue TBA")
                + " | " + price
                + (registered ? " | registered" : "");
    }

    private String eventDetailLine(Event event, boolean registered) {
        if (event == null) {
            return "Unknown event";
        }
        String description = truncate(nullSafe(event.getDescription(), "No description available"), 220);
        return eventLine(event, registered)
                + " | type " + (event.getType() == null ? "event" : event.getType().name())
                + " | description: " + description;
    }

    private String buildEventDescription(Event event) {
        String title = nullSafe(event.getTitle(), "This campus event");
        String venue = nullSafe(event.getVenue(), "the campus venue");
        String date = event.getDate() == null ? "a date to be announced" : DATE_FORMATTER.format(event.getDate());
        String type = event.getType() == null ? "campus" : event.getType().name().toLowerCase(Locale.ROOT);
        String existingDescription = event.getDescription();

        StringBuilder description = new StringBuilder();
        description.append("Here is a description for ").append(title).append(":\n\n");
        description.append("\"").append(title)
                .append(" is a ").append(type)
                .append(" event designed to bring students together for learning, networking, and meaningful campus engagement. ");
        description.append("Join us at ").append(venue)
                .append(" on ").append(date)
                .append(" for an experience that helps participants connect, explore opportunities, and take the next step in their campus journey.");
        if (existingDescription != null && !existingDescription.isBlank()) {
            description.append(" The event focus is: ").append(truncate(existingDescription.trim(), 160)).append(".");
        }
        description.append("\"");
        return description.toString();
    }

    private String roleGuidance(Role role) {
        if (role == Role.STUDENT) {
            return "Help discover events, understand feed activity, register or cancel event registrations, and manage profile actions.";
        }
        if (role == Role.SOCIETY || role == Role.DEPARTMENT) {
            return "Help create events, improve event posts, understand registrations, and check analytics for owned events.";
        }
        if (role == Role.ADMIN) {
            return "Help monitor platform activity, moderate content, and understand analytics.";
        }
        return "Help with general CampusSync navigation.";
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String normalizeForIntent(String message) {
        if (message == null) {
            return "";
        }
        return message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isGreeting(String normalized) {
        return normalized.equals("hi")
                || normalized.equals("hii")
                || normalized.equals("hiii")
                || normalized.equals("hello")
                || normalized.equals("hey")
                || normalized.equals("good morning")
                || normalized.equals("good afternoon")
                || normalized.equals("good evening")
                || normalized.equals("how are you")
                || normalized.equals("how r u")
                || normalized.equals("how are u")
                || normalized.startsWith("hello ")
                || normalized.startsWith("hey ")
                || normalized.startsWith("how are you ");
    }

    private boolean wantsCancelRegistration(String normalized) {
        return containsAny(normalized, "cancel registration", "unregister", "remove registration")
                || (containsAny(normalized, "cancel", "remove") && containsAny(normalized, "registered event", "my event"));
    }

    private boolean wantsRegistrations(String normalized) {
        return containsAny(normalized,
                "my registrations",
                "my registered events",
                "registered events",
                "events i registered",
                "events am i registered",
                "what events am i registered",
                "show my registrations",
                "show my registered events",
                "my event registrations")
                || (containsAny(normalized, "registered", "registration") && containsAny(normalized, "event", "events"));
    }

    private boolean wantsUpcomingEvents(String normalized) {
        return normalized.equals("events")
                || containsAny(normalized,
                "show events",
                "list events",
                "find events",
                "browse events",
                "upcoming events",
                "available events",
                "next events",
                "event list",
                "what events are available",
                "which events are upcoming");
    }

    private boolean wantsCreateEvent(String normalized) {
        return containsAny(normalized,
                "create event",
                "add event",
                "publish event",
                "host event",
                "organize event",
                "make event");
    }

    private boolean wantsCreatePost(String normalized) {
        return containsAny(normalized,
                "create post",
                "add post",
                "write post",
                "publish post",
                "upload post",
                "new post",
                "post on feed",
                "feed post");
    }

    private boolean wantsProfileHelp(String normalized) {
        return containsAny(normalized,
                "edit profile",
                "update profile",
                "change profile",
                "profile photo",
                "profile picture",
                "my profile");
    }

    private boolean wantsAnalytics(String normalized) {
        return normalized.equals("analytics")
                || normalized.equals("dashboard")
                || containsAny(normalized,
                "event analytics",
                "platform analytics",
                "analytics report",
                "view analytics",
                "open dashboard",
                "performance report",
                "engagement report");
    }

    private boolean wantsDescription(String normalized) {
        return containsAny(normalized,
                "write description",
                "create description",
                "generate description",
                "make description",
                "event description",
                "write caption",
                "create caption",
                "generate caption",
                "write announcement",
                "create announcement",
                "generate announcement");
    }

    private boolean containsAny(String normalized, String... phrases) {
        for (String phrase : phrases) {
            if (normalized.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private record ChatContext(List<Event> upcomingEvents, List<Registration> registrations) {
    }
}
