package com.TalentForge.talentforge.payment.dto;

import com.TalentForge.talentforge.payment.entity.BillingCycle;
import com.TalentForge.talentforge.payment.entity.PaymentCurrency;
import com.TalentForge.talentforge.subscription.entity.PlanType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PaymentInitializeRequest(
        @NotNull PlanType planType,
        @NotNull BillingCycle billingCycle,
        @NotNull PaymentCurrency currency,
        List<String> channels
) {
}
