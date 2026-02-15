package com.TalentForge.talentforge.integration.dto;

import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;
import jakarta.validation.constraints.NotNull;

public record IntegrationPublishRequest(
        @NotNull Long recruiterId,
        @NotNull Long jobId,
        @NotNull IntegrationPlatform platform,
        String customMessage
) {
}
