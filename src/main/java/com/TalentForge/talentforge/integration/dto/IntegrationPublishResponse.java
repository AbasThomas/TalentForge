package com.TalentForge.talentforge.integration.dto;

import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;
import com.TalentForge.talentforge.integration.entity.PublishStatus;

import java.time.LocalDateTime;

public record IntegrationPublishResponse(
        Long logId,
        Long recruiterId,
        Long jobId,
        IntegrationPlatform platform,
        String targetUrl,
        String shareText,
        PublishStatus status,
        String message,
        LocalDateTime publishedAt
) {
}
