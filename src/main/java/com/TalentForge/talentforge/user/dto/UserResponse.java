package com.TalentForge.talentforge.user.dto;

import com.TalentForge.talentforge.user.entity.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        UserRole secondaryRole,
        List<UserRole> availableRoles,
        String company,
        String phone,
        boolean active,
        boolean verified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
