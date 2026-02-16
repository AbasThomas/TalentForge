package com.TalentForge.talentforge.payment.dto;

import com.TalentForge.talentforge.payment.entity.BillingCycle;
import com.TalentForge.talentforge.payment.entity.PaymentCurrency;
import com.TalentForge.talentforge.payment.entity.PaymentStatus;
import com.TalentForge.talentforge.subscription.entity.PlanType;

import java.time.LocalDateTime;

public record PaymentHistoryItemResponse(
        Long id,
        String reference,
        PlanType planType,
        BillingCycle billingCycle,
        PaymentCurrency currency,
        Long amountMinor,
        Long amountUsdMinor,
        PaymentStatus status,
        String gatewayStatus,
        String gatewayResponse,
        String channel,
        String authorizationUrl,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
