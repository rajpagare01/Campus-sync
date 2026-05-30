package com.campussync.backend.Controller;

import com.campussync.backend.Dto.UserActivityResponse;
import com.campussync.backend.Dto.UserProfileRequest;
import com.campussync.backend.Dto.UserProfileResponse;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Service.UserService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "student@example.com")
    void getMyProfile_ShouldReturnProfile_WhenAuthenticated() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(1L);
        response.setName("Test User");
        response.setEmail("student@example.com");

        when(userService.getMyProfile()).thenReturn(response);

        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.email").value("student@example.com"));
    }

    @Test
    void getMyProfile_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserProfile_ShouldReturnProfile_WhenPublic() throws Exception {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(2L);
        response.setName("Public User");

        when(userService.getUserProfile(2L)).thenReturn(response);

        mockMvc.perform(get("/users/2/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Public User"));
    }

    @Test
    @WithMockUser(username = "student@example.com")
    void updateProfile_ShouldReturnUpdatedProfile_WhenValidRequest() throws Exception {
        UserProfileRequest request = new UserProfileRequest();
        request.setName("Updated Name");
        request.setBio("New bio");

        UserProfileResponse response = new UserProfileResponse();
        response.setId(1L);
        response.setName("Updated Name");
        response.setBio("New bio");

        when(userService.updateProfile(any(UserProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.bio").value("New bio"));
    }

    @Test
    @WithMockUser(username = "student@example.com")
    void getMyActivity_ShouldReturnActivityList() throws Exception {
        UserActivityResponse activity = new UserActivityResponse();
        activity.setId(10L);
        activity.setActivityType("LIKE");
        
        when(userService.getMyActivity()).thenReturn(List.of(activity));

        mockMvc.perform(get("/users/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].activityType").value("LIKE"));
    }
}
