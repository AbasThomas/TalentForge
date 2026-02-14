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
        String paymentReference,
        Boolean active
) {
}
