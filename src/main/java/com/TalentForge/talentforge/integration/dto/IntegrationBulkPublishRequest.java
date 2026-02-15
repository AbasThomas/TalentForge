package com.TalentForge.talentforge.integration.dto;

import jakarta.validation.constraints.NotNull;

public record IntegrationBulkPublishRequest(
        @NotNull Long recruiterId,
        @NotNull Long jobId,
        String customMessage
) {
}
