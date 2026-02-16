package com.TalentForge.talentforge.subscription.dto;

import com.TalentForge.talentforge.subscription.entity.PlanType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SubscriptionRequest(
        @NotNull Long userId,
        @NotNull PlanType planType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer jobPostLimit,
        Integer applicantLimit,
        Integer applicationLimit,
        Integer resumeScoreLimit,
        Integer applicationUsed,
        Integer resumeScoreUsed,
        String paymentReference,
        Boolean active
) {
}
