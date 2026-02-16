package com.TalentForge.talentforge.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String fullName,
        String company,
        String phone
) {
}
