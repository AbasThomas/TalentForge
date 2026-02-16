package com.TalentForge.talentforge.payment.dto;

import com.TalentForge.talentforge.payment.entity.BillingCycle;
import com.TalentForge.talentforge.payment.entity.PaymentCurrency;
import com.TalentForge.talentforge.subscription.entity.PlanType;

import java.util.List;

public record PaymentOptionsResponse(
        List<PaymentCurrency> currencies,
        List<String> channels,
        List<PaymentPriceOption> prices
) {
    public record PaymentPriceOption(
            PlanType planType,
            BillingCycle billingCycle,
            PaymentCurrency currency,
            Long amountMinor,
            Long amountUsdMinor,
            boolean enabled,
            String note
    ) {
    }
}
