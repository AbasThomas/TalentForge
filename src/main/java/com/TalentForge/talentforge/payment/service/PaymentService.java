package com.TalentForge.talentforge.payment.service;

import com.TalentForge.talentforge.payment.dto.PaymentInitializeRequest;
import com.TalentForge.talentforge.payment.dto.PaymentInitializeResponse;
import com.TalentForge.talentforge.payment.dto.PaymentOptionsResponse;
import com.TalentForge.talentforge.payment.dto.PaymentVerifyResponse;

public interface PaymentService {
    PaymentOptionsResponse getOptions();

    PaymentInitializeResponse initialize(PaymentInitializeRequest request);

    PaymentVerifyResponse verify(String reference);

    void processWebhook(String signature, String payload);
}
