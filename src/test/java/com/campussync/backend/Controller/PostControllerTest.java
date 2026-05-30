package com.campussync.backend.Controller;

import com.campussync.backend.Dto.PaginatedResponse;
import com.campussync.backend.Dto.PostRequest;
import com.campussync.backend.Dto.PostResponse;
import com.campussync.backend.Repository.UserRepository;
import com.campussync.backend.Service.PostService;
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

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class PostControllerTest {

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
    @WithMockUser(username = "society", roles = "SOCIETY")
    void createPost_ShouldReturnCreatedPost() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("New Post");

        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setContent("New Post");

        when(postService.createPost(any(PostRequest.class))).thenReturn(response);

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("New Post"));
    }

    @Test
    @WithMockUser
    void getAllPosts_ShouldReturnPaginatedPosts() throws Exception {
        PaginatedResponse<PostResponse> response = new PaginatedResponse<>();
        response.setContent(List.of(new PostResponse()));
        when(postService.getAllPosts(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/posts?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser
    void getPostById_ShouldReturnPost() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(1L);
        when(postService.getPostById(1L)).thenReturn(response);

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePost_ShouldReturnUpdatedPost() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("Updated Post");

        PostResponse response = new PostResponse();
        response.setId(1L);
        response.setContent("Updated Post");

        when(postService.updatePost(eq(1L), any(PostRequest.class))).thenReturn(response);

        mockMvc.perform(put("/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated Post"));
    }

    @Test
    @WithMockUser
    void getPostsByAuthor_ShouldReturnPaginatedPosts() throws Exception {
        PaginatedResponse<PostResponse> response = new PaginatedResponse<>();
        response.setContent(List.of(new PostResponse()));
        when(postService.getPostsByAuthor(eq(1L), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/posts/author/1?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser
    void getPostsWithMedia_ShouldReturnPaginatedPosts() throws Exception {
        PaginatedResponse<PostResponse> response = new PaginatedResponse<>();
        response.setContent(List.of(new PostResponse()));
        when(postService.getPostsWithMedia(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/posts/media?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deletePost_ShouldReturnSuccessString() throws Exception {
        mockMvc.perform(delete("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Post deleted successfully"));
    }
}
