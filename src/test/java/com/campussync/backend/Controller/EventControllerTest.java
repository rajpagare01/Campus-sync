package com.campussync.backend.Controller;

import com.campussync.backend.Dto.EventResponse;
import com.campussync.backend.Dto.PaginatedEventResponse;
import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Service.EventService;
import com.campussync.backend.config.JwtFilter;
import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class EventControllerTest {

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
    void getAllEvents_ShouldReturnPaginatedEvents() throws Exception {
        PaginatedEventResponse response = new PaginatedEventResponse();
        response.setContent(List.of(new EventResponse()));
        when(eventService.getAllEvents(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/events?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getEventById_ShouldReturnEvent() throws Exception {
        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setTitle("Test Event");
        when(eventService.getEventById(1L)).thenReturn(response);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Test Event"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateEvent_ShouldReturnUpdatedEvent() throws Exception {
        Event event = new Event();
        event.setTitle("Updated Title");

        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setTitle("Updated Title");

        when(eventService.updateEvent(eq(1L), any(Event.class))).thenReturn(response);

        mockMvc.perform(put("/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteEvent_ShouldReturnSuccessMessage() throws Exception {
        when(eventService.deleteEvent(1L)).thenReturn("Event deleted successfully");

        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Event deleted successfully"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateEventStatus_ShouldReturnUpdatedStatus() throws Exception {
        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setStatus(EventStatus.PUBLISHED);

        when(eventService.updateEventStatus(1L, EventStatus.PUBLISHED)).thenReturn(response);

        mockMvc.perform(patch("/events/1/status?status=PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void searchEvents_ShouldReturnPaginatedResults() throws Exception {
        PaginatedEventResponse response = new PaginatedEventResponse();
        response.setContent(List.of(new EventResponse()));
        when(eventService.searchEvents(eq("tech"), any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/events/search?keyword=tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
