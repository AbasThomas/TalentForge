package com.TalentForge.talentforge.payment.service;

import com.TalentForge.talentforge.payment.dto.PaymentInitializeRequest;
import com.TalentForge.talentforge.payment.dto.PaymentInitializeResponse;
import com.TalentForge.talentforge.payment.dto.PaymentHistoryItemResponse;
import com.TalentForge.talentforge.payment.dto.PaymentOptionsResponse;
import com.TalentForge.talentforge.payment.dto.PaymentVerifyResponse;

import java.util.List;

public interface PaymentService {
    PaymentOptionsResponse getOptions();

    PaymentInitializeResponse initialize(PaymentInitializeRequest request);

    PaymentVerifyResponse verify(String reference);

    List<PaymentHistoryItemResponse> getHistory();

    void processWebhook(String signature, String payload);
}
