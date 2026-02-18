package com.TalentForge.talentforge.admin.promotion.dto;

import com.TalentForge.talentforge.admin.promotion.entity.PromoRepostStatus;
import jakarta.validation.constraints.NotNull;

public record PromoRepostStatusRequest(
        @NotNull Long jobId,
        @NotNull PromoRepostStatus status
) {
}
