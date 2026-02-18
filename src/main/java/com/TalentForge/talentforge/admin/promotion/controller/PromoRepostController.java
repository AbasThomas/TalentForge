package com.TalentForge.talentforge.admin.promotion.controller;

import com.TalentForge.talentforge.admin.promotion.dto.PromoRepostConsentRequest;
import com.TalentForge.talentforge.admin.promotion.dto.PromoRepostResponse;
import com.TalentForge.talentforge.admin.promotion.dto.PromoRepostStatusRequest;
import com.TalentForge.talentforge.admin.promotion.service.PromoRepostService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promotions/reposts")
@RequiredArgsConstructor
public class PromoRepostController {

    private final PromoRepostService promoRepostService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoRepostResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<PromoRepostResponse>>builder()
                .success(true)
                .message("Promo reposts fetched")
                .data(promoRepostService.getAll())
                .build());
    }

    @PutMapping("/consent")
    public ResponseEntity<ApiResponse<PromoRepostResponse>> setConsent(@Valid @RequestBody PromoRepostConsentRequest request) {
        return ResponseEntity.ok(ApiResponse.<PromoRepostResponse>builder()
                .success(true)
                .message("Promo repost consent updated")
                .data(promoRepostService.setConsent(request.jobId(), request.consentGiven()))
                .build());
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponse<PromoRepostResponse>> setStatus(@Valid @RequestBody PromoRepostStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.<PromoRepostResponse>builder()
                .success(true)
                .message("Promo repost status updated")
                .data(promoRepostService.setStatus(request.jobId(), request.status()))
                .build());
    }
}
