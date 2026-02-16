package com.TalentForge.talentforge.payment.dto;

import com.TalentForge.talentforge.payment.entity.BillingCycle;
import com.TalentForge.talentforge.payment.entity.PaymentCurrency;
import com.TalentForge.talentforge.subscription.entity.PlanType;

import java.util.List;

public record PaymentInitializeResponse(
        String reference,
        String authorizationUrl,
        String accessCode,
        PlanType planType,
        BillingCycle billingCycle,
        PaymentCurrency currency,
        Long amountMinor,
        Long amountUsdMinor,
        List<String> channels
) {
}
