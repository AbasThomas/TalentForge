package com.TalentForge.talentforge.user.dto;

import com.TalentForge.talentforge.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleSwitchRequest(
        @NotNull(message = "role is required")
        UserRole role,
        Boolean addIfMissing
) {
}
