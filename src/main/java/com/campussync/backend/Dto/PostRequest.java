package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest {
    
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    private String content;
    
    @Size(max = 1000, message = "Media URL must not exceed 1000 characters")
    private String mediaUrl;
    
    private Long eventId; // Optional: link to event
}
