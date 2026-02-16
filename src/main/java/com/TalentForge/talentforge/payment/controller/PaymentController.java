package com.TalentForge.talentforge.payment.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.payment.dto.PaymentInitializeRequest;
import com.TalentForge.talentforge.payment.dto.PaymentInitializeResponse;
import com.TalentForge.talentforge.payment.dto.PaymentOptionsResponse;
import com.TalentForge.talentforge.payment.dto.PaymentVerifyResponse;
import com.TalentForge.talentforge.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<PaymentOptionsResponse>> options() {
        return ResponseEntity.ok(ApiResponse.<PaymentOptionsResponse>builder()
                .success(true)
                .message("Payment options fetched")
                .data(paymentService.getOptions())
                .build());
    }

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<PaymentInitializeResponse>> initialize(@Valid @RequestBody PaymentInitializeRequest request) {
        return ResponseEntity.ok(ApiResponse.<PaymentInitializeResponse>builder()
                .success(true)
                .message("Payment initialized")
                .data(paymentService.initialize(request))
                .build());
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<ApiResponse<PaymentVerifyResponse>> verify(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.<PaymentVerifyResponse>builder()
                .success(true)
                .message("Payment verification completed")
                .data(paymentService.verify(reference))
                .build());
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Object>> webhook(
            @RequestHeader(name = "x-paystack-signature", required = false) String signature,
            @RequestBody String payload
    ) {
        paymentService.processWebhook(signature, payload);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Webhook processed")
                .data(null)
                .build());
    }
}
