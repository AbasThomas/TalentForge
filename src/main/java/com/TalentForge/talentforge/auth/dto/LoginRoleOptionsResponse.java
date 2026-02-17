package com.TalentForge.talentforge.auth.dto;

import com.TalentForge.talentforge.user.entity.UserRole;

import java.util.List;

public record LoginRoleOptionsResponse(
        List<UserRole> availableRoles
) {
}
