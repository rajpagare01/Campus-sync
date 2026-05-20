package com.campussync.backend.Controller;

import com.campussync.backend.Dto.AiChatRequest;
import com.campussync.backend.Dto.AiChatResponse;
import com.campussync.backend.Dto.AiConversationDetailResponse;
import com.campussync.backend.Dto.AiConversationSummary;
import com.campussync.backend.Service.AIService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping({"/ai/generate", "/api/v1/ai/generate"})
    public String generate(@RequestParam String prompt) {
        return aiService.generate(prompt);
    }

    @PostMapping({"/api/ai/chat", "/api/v1/ai/chat"})
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiService.chat(request);
    }

    @GetMapping({"/api/ai/conversations", "/api/v1/ai/conversations"})
    @PreAuthorize("isAuthenticated()")
    public List<AiConversationSummary> getConversations() {
        return aiService.getConversations();
    }

    @GetMapping({"/api/ai/conversations/{conversationId}", "/api/v1/ai/conversations/{conversationId}"})
    @PreAuthorize("isAuthenticated()")
    public AiConversationDetailResponse getConversation(@PathVariable Long conversationId) {
        return aiService.getConversation(conversationId);
    }

    @DeleteMapping({"/api/ai/conversations/{conversationId}", "/api/v1/ai/conversations/{conversationId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deleteConversation(@PathVariable Long conversationId) {
        aiService.deleteConversation(conversationId);
    }

    @PostMapping({"/api/ai/conversations/{conversationId}/regenerate", "/api/v1/ai/conversations/{conversationId}/regenerate"})
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse regenerateAnswer(@PathVariable Long conversationId) {
        return aiService.regenerate(conversationId);
    }

    @PostMapping({"/api/ai/chat/stream", "/api/v1/ai/chat/stream"})
    @PreAuthorize("isAuthenticated()")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(@Valid @RequestBody AiChatRequest request) {
        return aiService.streamChat(request);
    }
}
