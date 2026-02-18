package com.TalentForge.talentforge.admin.promotion.dto;

import jakarta.validation.constraints.NotNull;

public record PromoRepostConsentRequest(
        @NotNull Long jobId,
        @NotNull Boolean consentGiven
) {
}
