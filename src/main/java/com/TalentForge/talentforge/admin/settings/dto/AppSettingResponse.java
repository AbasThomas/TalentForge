package com.TalentForge.talentforge.admin.settings.dto;

import java.time.LocalDateTime;

public record AppSettingResponse(
        Long id,
        String key,
        String value,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
