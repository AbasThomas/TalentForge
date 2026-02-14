package com.TalentForge.talentforge.auth.dto;

import com.TalentForge.talentforge.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName,
        UserRole role,
        String company,
        String phone
) {
}
