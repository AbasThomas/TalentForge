package com.TalentForge.talentforge.subscription.mapper;

import com.TalentForge.talentforge.subscription.dto.SubscriptionResponse;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getUser().getId(),
                subscription.getUser().getEmail(),
                subscription.getPlanType(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.isActive(),
                subscription.getJobPostLimit(),
                subscription.getApplicantLimit(),
                subscription.getPaymentReference(),
                subscription.getCreatedAt()
        );
    }
}
