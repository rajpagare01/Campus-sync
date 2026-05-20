package com.campussync.backend.Controller;

import com.campussync.backend.config.JwtFilter;
import com.campussync.backend.config.JwtUtil;
import com.campussync.backend.config.SecurityConfig;
import com.campussync.backend.Dto.PostRequest;
import com.campussync.backend.Dto.PostResponse;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class PostControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void studentCanCreatePost() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setContent("Student announcement");
        response.setAuthorId(10L);
        response.setAuthorName("Student User");
        response.setCreatedAt(LocalDateTime.of(2026, 4, 20, 0, 0));

        when(postService.createPost(any(PostRequest.class))).thenReturn(response);

        PostRequest request = new PostRequest("Student announcement", null, null);

        mockMvc.perform(post("/api/posts")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.content").value("Student announcement"));
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void authenticatedUserCanUpdatePost() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setContent("Updated post");
        response.setAuthorId(10L);
        response.setAuthorName("Student User");
        response.setUpdatedAt(LocalDateTime.of(2026, 4, 20, 1, 0));

        when(postService.updatePost(eq(1L), any(PostRequest.class))).thenReturn(response);

        PostRequest request = new PostRequest("Updated post", null, null);

        mockMvc.perform(put("/api/posts/1")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.content").value("Updated post"));
    }
}
