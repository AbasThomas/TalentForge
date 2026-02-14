package com.TalentForge.talentforge.auth.dto;

import com.TalentForge.talentforge.user.dto.UserResponse;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
