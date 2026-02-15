package com.TalentForge.talentforge.integration.dto;

import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;

import java.time.LocalDateTime;

public record IntegrationConnectionResponse(
        Long id,
        Long recruiterId,
        IntegrationPlatform platform,
        boolean connected,
        String accountHandle,
        String profileUrl,
        String defaultMessage,
        LocalDateTime connectedAt,
        LocalDateTime updatedAt
) {
}
