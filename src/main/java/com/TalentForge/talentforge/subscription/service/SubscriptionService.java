package com.TalentForge.talentforge.subscription.service;

import com.TalentForge.talentforge.subscription.dto.SubscriptionRequest;
import com.TalentForge.talentforge.subscription.dto.SubscriptionResponse;
import com.TalentForge.talentforge.subscription.dto.SubscriptionUsageResponse;

public interface SubscriptionService {
    SubscriptionResponse upsert(SubscriptionRequest request);

    SubscriptionResponse getByUserId(Long userId);

    SubscriptionUsageResponse getUsageByUserEmail(String email);
}
