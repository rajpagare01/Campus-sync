package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String profilePicture;
    private String bio;
    private String createdAt;
}
