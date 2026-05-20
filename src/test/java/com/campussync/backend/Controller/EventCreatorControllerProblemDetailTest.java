package com.campussync.backend.Controller;

import com.campussync.backend.config.JwtFilter;
import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.config.SecurityConfig;
import com.campussync.backend.Exception.ConflictException;
import com.campussync.backend.Exception.ForbiddenOperationException;
import com.campussync.backend.Exception.GlobalExceptionHandler;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Service.EventParticipantService;
import com.campussync.backend.Service.FeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventCreatorController.class)
@Import({SecurityConfig.class, JwtFilter.class, GlobalExceptionHandler.class})
class EventCreatorControllerProblemDetailTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventParticipantService eventParticipantService;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;


    @Test
    @WithMockUser(username = "creator@example.com", roles = "SOCIETY")
    void invalidFeedbackRequestReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/events/12/feedback")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FeedbackPayload(6, "Too high"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.rating").exists());
    }

    @Test
    @WithMockUser(username = "creator@example.com", roles = "SOCIETY")
    void checkInConflictReturnsProblemDetail() throws Exception {
        when(eventParticipantService.checkIn(any()))
                .thenThrow(new ConflictException("Participant already checked in"));

        mockMvc.perform(post("/events/check-in")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"qrCode":"signed-qr"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Participant already checked in"))
                .andExpect(jsonPath("$.type").value("https://api.campussync.local/problems/conflict"));
    }

    @Test
    @WithMockUser(username = "creator@example.com", roles = "SOCIETY")
    void certificateAccessRuleReturnsProblemDetail() throws Exception {
        when(eventParticipantService.generateCertificate(12L, 2L))
                .thenThrow(new ForbiddenOperationException("Certificate is available only for attended participants"));

        mockMvc.perform(get("/events/12/certificate/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Certificate is available only for attended participants"))
                .andExpect(jsonPath("$.type").value("https://api.campussync.local/problems/forbidden-operation"));
    }

    private record FeedbackPayload(int rating, String comment) {
    }
}
