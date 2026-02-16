package com.TalentForge.talentforge.subscription.dto;

import com.TalentForge.talentforge.subscription.entity.PlanType;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        Long userId,
        String userEmail,
        PlanType planType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean active,
        Integer jobPostLimit,
        Integer applicantLimit,
        Integer applicationLimit,
        Integer resumeScoreLimit,
        Integer applicationUsed,
        Integer resumeScoreUsed,
        String paymentReference,
        LocalDateTime createdAt
) {
}
