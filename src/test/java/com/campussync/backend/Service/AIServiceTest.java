package com.campussync.backend.Service;

import com.campussync.backend.Dto.AiChatRequest;
import com.campussync.backend.Dto.AiChatResponse;
import com.campussync.backend.Dto.AiConversationDetailResponse;
import com.campussync.backend.Dto.AiConversationSummary;
import com.campussync.backend.Exception.AiProviderException;
import com.campussync.backend.Model.AiConversation;
import com.campussync.backend.Model.AiMessage;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Model.Registration;
import com.campussync.backend.Model.RegistrationStatus;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.AiConversationRepository;
import com.campussync.backend.Repository.AiMessageRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.RegistrationRepository;
import com.campussync.backend.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIServiceTest {

    private AiConversationRepository conversationRepository;
    private AiMessageRepository messageRepository;
    private UserRepository userRepository;
    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private RestTemplate restTemplate;
    private WebClient webClient;
    private ObjectMapper objectMapper;
    private AIService aiService;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(AiConversationRepository.class);
        messageRepository = mock(AiMessageRepository.class);
        userRepository = mock(UserRepository.class);
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        restTemplate = mock(RestTemplate.class);
        webClient = mock(WebClient.class);
        objectMapper = new ObjectMapper();

        aiService = new AIService(
                conversationRepository,
                messageRepository,
                userRepository,
                eventRepository,
                registrationRepository,
                restTemplate,
                webClient,
                objectMapper
        );
        ReflectionTestUtils.setField(aiService, "geminiApiKey", "test-gemini-key");
        ReflectionTestUtils.setField(aiService, "geminiApiBaseUrl", "https://generativelanguage.googleapis.com/v1beta/models");
        ReflectionTestUtils.setField(aiService, "geminiModel", "gemini-2.5-flash");
        ReflectionTestUtils.setField(aiService, "failFastOnProviderError", false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatReturnsContextualGuidanceWhenGeminiIsUnavailable() {
        User user = studentUser();
        Event event = upcomingEvent();
        Registration registration = registration(user, event);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(100L);
            }
            return conversation;
        });
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(registrationRepository.findByUserId(user.getId())).thenReturn(List.of(registration));
        when(restTemplate.postForEntity(eq(geminiUrl()), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("offline"));

        AiChatRequest request = new AiChatRequest();
        request.setMessage("Any hackathon options if AI is offline?");

        AiChatResponse response = aiService.chat(request);

        assertThat(response.isFallback()).isTrue();
        assertThat(response.getConversationId()).isEqualTo(100L);
        assertThat(response.getAnswer()).contains("Campus Hackathon");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getId()).isEqualTo(10L);
        assertThat(response.getSources().get(0).isRegistered()).isTrue();

        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues()).extracting(AiMessage::getRole)
                .containsExactly(AiMessage.Role.USER, AiMessage.Role.ASSISTANT);
    }

    @Test
    void chatThrowsProviderExceptionWhenGeminiFailsAndFailFastIsEnabled() {
        User user = studentUser();
        Event event = upcomingEvent();

        ReflectionTestUtils.setField(aiService, "failFastOnProviderError", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(104L);
            }
            return conversation;
        });
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(104L)).thenReturn(List.of());
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(registrationRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq(geminiUrl()), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Gemini connection failed"));

        AiChatRequest request = new AiChatRequest();
        request.setMessage("hello");

        assertThatThrownBy(() -> aiService.chat(request))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Gemini API failed")
                .hasMessageContaining("Gemini connection failed");
    }

    @Test
    void chatFallsBackWhenGeminiKeyIsMissing() {
        User user = studentUser();
        Event event = upcomingEvent();
        Registration registration = registration(user, event);

        ReflectionTestUtils.setField(aiService, "geminiApiKey", "");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(105L);
            }
            return conversation;
        });
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(105L)).thenReturn(List.of());
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(registrationRepository.findByUserId(user.getId())).thenReturn(List.of(registration));

        AiChatRequest request = new AiChatRequest();
        request.setMessage("What events am I registered for?");

        AiChatResponse response = aiService.chat(request);

        assertThat(response.isFallback()).isTrue();
        assertThat(response.getConversationId()).isEqualTo(105L);
        assertThat(response.getAnswer()).contains("registered");
        verify(restTemplate, times(0)).postForEntity(eq(geminiUrl()), any(), eq(Map.class));
    }

    @Test
    void chatSendsRegistrationQuestionsToGeminiInsteadOfUsingPredefinedReply() {
        User user = studentUser();
        Event event = upcomingEvent();
        Registration registration = registration(user, event);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(102L);
            }
            return conversation;
        });
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(102L)).thenReturn(List.of());
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(registrationRepository.findByUserId(user.getId())).thenReturn(List.of(registration));
        when(restTemplate.postForEntity(eq(geminiUrl()), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiResponse("You are registered for Campus Hackathon.")));

        AiChatRequest request = new AiChatRequest();
        request.setMessage("Show my registered events");

        AiChatResponse response = aiService.chat(request);

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getConversationId()).isEqualTo(102L);
        assertThat(response.getAnswer()).isEqualTo("You are registered for Campus Hackathon.");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).isRegistered()).isTrue();

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(geminiUrl()), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getHeaders().getFirst("x-goog-api-key")).isEqualTo("test-gemini-key");
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("Do not use predefined")
                .contains("Current user registrations")
                .contains("Campus Hackathon");
    }

    @Test
    void chatBuildsRoleAwarePromptAndUsesGeminiResponse() {
        User user = new User();
        user.setId(2L);
        user.setName("Organizer User");
        user.setEmail("organizer@example.com");
        user.setRole(Role.SOCIETY);

        Event event = upcomingEvent();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(101L);
            }
            return conversation;
        });
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(101L)).thenReturn(List.of());
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(registrationRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq(geminiUrl()), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiResponse("Use a clear title and publish the event.")));

        AiChatRequest request = new AiChatRequest();
        request.setMessage("How do I improve my event?");

        AiChatResponse response = aiService.chat(request);

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getAnswer()).isEqualTo("Use a clear title and publish the event.");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(geminiUrl()), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("Role: SOCIETY")
                .contains("Help create events")
                .contains("Campus Hackathon");
    }

    @Test
    void chatIncludesFocusedEventFromHistoryForDescriptionRequests() {
        User user = studentUser();
        Event event = upcomingEvent();
        AiMessage previousAssistantMessage = new AiMessage();
        previousAssistantMessage.setRole(AiMessage.Role.ASSISTANT);
        previousAssistantMessage.setContent("I found Campus Hackathon at Innovation Lab.");
        previousAssistantMessage.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(103L);
            }
            return conversation;
        });
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(103L)).thenReturn(List.of(previousAssistantMessage));
        when(eventRepository.findByStatusOrderByDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(registrationRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(restTemplate.postForEntity(eq(geminiUrl()), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(geminiResponse("Here is a description for Campus Hackathon.")));

        AiChatRequest request = new AiChatRequest();
        request.setMessage("write description");

        AiChatResponse response = aiService.chat(request);

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getAnswer()).isEqualTo("Here is a description for Campus Hackathon.");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(geminiUrl()), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("Event currently in focus")
                .contains("Campus Hackathon")
                .contains("If the user asks to write a description");
    }

    @Test
    void getConversationsReturnsSummariesForCurrentUser() {
        User user = studentUser();
        AiConversation newestConversation = conversation(201L, user, "Hackathon planning", LocalDateTime.now().minusHours(3));
        AiConversation olderConversation = conversation(200L, user, "Profile help", LocalDateTime.now().minusDays(1));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()))
                .thenReturn(List.of(newestConversation, olderConversation));
        when(messageRepository.countByConversationId(201L)).thenReturn(4L);
        when(messageRepository.countByConversationId(200L)).thenReturn(2L);
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(201L))
                .thenReturn(Optional.of(message(newestConversation, user, AiMessage.Role.ASSISTANT, "A".repeat(140), LocalDateTime.now())));
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(200L))
                .thenReturn(Optional.of(message(olderConversation, user, AiMessage.Role.USER, "Update my bio", LocalDateTime.now().minusHours(4))));

        List<AiConversationSummary> summaries = aiService.getConversations();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).getConversationId()).isEqualTo(201L);
        assertThat(summaries.get(0).getMessageCount()).isEqualTo(4L);
        assertThat(summaries.get(0).getLastMessagePreview()).hasSize(120).endsWith("...");
        assertThat(summaries.get(1).getConversationId()).isEqualTo(200L);
        assertThat(summaries.get(1).getLastMessagePreview()).isEqualTo("Update my bio");
    }

    @Test
    void getConversationReturnsStoredMessagesForOwnedConversation() {
        User user = studentUser();
        AiConversation conversation = conversation(300L, user, "Event ideas", LocalDateTime.now().minusHours(2));
        AiMessage firstMessage = message(conversation, user, AiMessage.Role.USER, "Show me events", LocalDateTime.now().minusMinutes(5));
        firstMessage.setId(1L);
        AiMessage secondMessage = message(conversation, user, AiMessage.Role.ASSISTANT, "Campus Hackathon is coming up.", LocalDateTime.now().minusMinutes(4));
        secondMessage.setId(2L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(conversation.getId(), user.getId()))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()))
                .thenReturn(List.of(firstMessage, secondMessage));

        AiConversationDetailResponse response = aiService.getConversation(conversation.getId());

        assertThat(response.getConversationId()).isEqualTo(300L);
        assertThat(response.getTitle()).isEqualTo("Event ideas");
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages()).extracting("role")
                .containsExactly(AiMessage.Role.USER, AiMessage.Role.ASSISTANT);
        assertThat(response.getMessages()).extracting("content")
                .containsExactly("Show me events", "Campus Hackathon is coming up.");
    }

    @Test
    void deleteConversationRemovesMessagesBeforeConversation() {
        User user = studentUser();
        AiConversation conversation = conversation(400L, user, "Old chat", LocalDateTime.now().minusDays(2));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdAndUserId(conversation.getId(), user.getId()))
                .thenReturn(Optional.of(conversation));

        aiService.deleteConversation(conversation.getId());

        verify(messageRepository, times(1)).deleteByConversationId(conversation.getId());
        verify(conversationRepository, times(1)).delete(conversation);
    }

    private String geminiUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    }

    private Map<String, Object> geminiResponse(String text) {
        return Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", text))
                        )
                ))
        );
    }

    private User studentUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Student User");
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);
        return user;
    }

    private Event upcomingEvent() {
        Event event = new Event();
        event.setId(10L);
        event.setTitle("Campus Hackathon");
        event.setVenue("Innovation Lab");
        event.setDate(LocalDateTime.now().plusDays(3));
        event.setType(EventType.SOCIETY);
        event.setStatus(EventStatus.PUBLISHED);
        event.setPaid(false);
        return event;
    }

    private Registration registration(User user, Event event) {
        Registration registration = new Registration();
        registration.setId(50L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);
        return registration;
    }

    private AiConversation conversation(Long id, User user, String title, LocalDateTime updatedAt) {
        AiConversation conversation = new AiConversation();
        conversation.setId(id);
        conversation.setUser(user);
        conversation.setTitle(title);
        conversation.setCreatedAt(updatedAt.minusHours(1));
        conversation.setUpdatedAt(updatedAt);
        return conversation;
    }

    private AiMessage message(
            AiConversation conversation,
            User user,
            AiMessage.Role role,
            String content,
            LocalDateTime createdAt
    ) {
        AiMessage message = new AiMessage();
        message.setConversation(conversation);
        message.setUser(user);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(createdAt);
        return message;
    }
}
