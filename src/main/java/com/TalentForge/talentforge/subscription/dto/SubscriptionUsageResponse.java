package com.TalentForge.talentforge.subscription.dto;

import com.TalentForge.talentforge.subscription.entity.PlanType;

import java.time.LocalDateTime;

public record SubscriptionUsageResponse(
        PlanType planType,
        boolean active,
        LocalDateTime endDate,
        Integer jobPostLimit,
        Integer jobPostUsed,
        Integer jobPostRemaining,
        Integer applicantLimit,
        Integer applicantUsed,
        Integer applicantRemaining,
        Integer applicationLimit,
        Integer applicationUsed,
        Integer applicationRemaining,
        Integer resumeScoreLimit,
        Integer resumeScoreUsed,
        Integer resumeScoreRemaining,
        String paymentReference
) {
}
