package com.TalentForge.talentforge.auth.dto;

import com.TalentForge.talentforge.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleSelectionRequest(
        @NotNull UserRole role
) {
}
