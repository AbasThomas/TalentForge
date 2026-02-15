package com.TalentForge.talentforge.integration.dto;

import jakarta.validation.constraints.NotNull;

public record IntegrationConnectionRequest(
        @NotNull Long recruiterId,
        String accountHandle,
        String profileUrl,
        String defaultMessage,
        Boolean connected
) {
}
