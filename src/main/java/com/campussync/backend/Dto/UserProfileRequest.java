package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileRequest {
    
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
    
    @Size(max = 1000, message = "Profile picture URL must not exceed 1000 characters")
    private String profilePictureUrl;

    @Size(max = 255, message = "Department must not exceed 255 characters")
    private String department;

    @Size(max = 50, message = "Year must not exceed 50 characters")
    private String year;
}
