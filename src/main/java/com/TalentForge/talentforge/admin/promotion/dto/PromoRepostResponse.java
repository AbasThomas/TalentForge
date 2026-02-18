package com.TalentForge.talentforge.admin.promotion.dto;

import com.TalentForge.talentforge.admin.promotion.entity.PromoRepostStatus;

import java.time.LocalDateTime;

public record PromoRepostResponse(
        Long id,
        Long jobId,
        boolean consentGiven,
        PromoRepostStatus status,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
