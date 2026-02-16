package com.TalentForge.talentforge.subscription.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.subscription.dto.SubscriptionRequest;
import com.TalentForge.talentforge.subscription.dto.SubscriptionResponse;
import com.TalentForge.talentforge.subscription.dto.SubscriptionUsageResponse;
import com.TalentForge.talentforge.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> upsert(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.<SubscriptionResponse>builder()
                .success(true)
                .message("Subscription saved")
                .data(subscriptionService.upsert(request))
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<SubscriptionResponse>builder()
                .success(true)
                .message("Subscription fetched")
                .data(subscriptionService.getByUserId(userId))
                .build());
    }

    @GetMapping("/me/usage")
    public ResponseEntity<ApiResponse<SubscriptionUsageResponse>> getMyUsage(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<SubscriptionUsageResponse>builder()
                .success(true)
                .message("Subscription usage fetched")
                .data(subscriptionService.getUsageByUserEmail(authentication.getName()))
                .build());
    }
}
