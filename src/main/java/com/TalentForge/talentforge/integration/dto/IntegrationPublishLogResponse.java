package com.TalentForge.talentforge.integration.dto;

import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;
import com.TalentForge.talentforge.integration.entity.PublishStatus;

import java.time.LocalDateTime;

public record IntegrationPublishLogResponse(
        Long id,
        Long recruiterId,
        Long jobId,
        IntegrationPlatform platform,
        String targetUrl,
        String shareText,
        PublishStatus status,
        String message,
        LocalDateTime createdAt
) {
}
