package com.campussync.backend.Dto;

import com.campussync.backend.Model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminRoleChangeRequest {
    @NotNull
    private Role role;
}
