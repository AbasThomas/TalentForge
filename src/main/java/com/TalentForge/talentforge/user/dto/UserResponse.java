package com.TalentForge.talentforge.user.dto;

import com.TalentForge.talentforge.user.entity.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        String company,
        String phone,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
