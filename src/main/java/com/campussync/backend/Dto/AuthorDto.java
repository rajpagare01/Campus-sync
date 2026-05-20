package com.campussync.backend.Dto;

import lombok.Data;

@Data
public class AuthorDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;
    private String profilePicture;
    private String avatarUrl;
}
