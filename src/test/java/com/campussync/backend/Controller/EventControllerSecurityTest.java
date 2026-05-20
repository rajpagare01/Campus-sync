package com.campussync.backend.Controller;

import com.campussync.backend.config.JwtFilter;
import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.config.SecurityConfig;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class EventControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void studentCanCreateEvent() throws Exception {
        com.campussync.backend.Dto.EventResponse response = new com.campussync.backend.Dto.EventResponse();
        response.setId(1L);
        response.setTitle("Student Event");
        response.setDescription("Open to everyone");
        response.setVenue("Auditorium");
        response.setDate(LocalDateTime.of(2026, 4, 21, 10, 0));
        response.setType(com.campussync.backend.Model.EventType.SOCIETY);
        response.setStatus(com.campussync.backend.Model.EventStatus.DRAFT);

        when(eventService.createEvent(any(Event.class))).thenReturn(response);

        Event request = new Event();
        request.setTitle("Student Event");
        request.setDescription("Open to everyone");
        request.setVenue("Auditorium");
        request.setDate(LocalDateTime.of(2026, 4, 21, 10, 0));
        request.setType(EventType.SOCIETY);

        mockMvc.perform(post("/events")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Student Event"));
    }
}
