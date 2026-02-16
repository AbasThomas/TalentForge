package com.TalentForge.talentforge.subscription.service;

import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.subscription.dto.SubscriptionRequest;
import com.TalentForge.talentforge.subscription.dto.SubscriptionResponse;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import com.TalentForge.talentforge.subscription.mapper.SubscriptionMapper;
import com.TalentForge.talentforge.subscription.repository.SubscriptionRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    public SubscriptionResponse upsert(SubscriptionRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));

        Subscription subscription = subscriptionRepository.findByUserId(request.userId())
                .orElse(Subscription.builder().user(user).build());

        subscription.setPlanType(request.planType());
        subscription.setStartDate(request.startDate() == null ? LocalDateTime.now() : request.startDate());
        subscription.setEndDate(request.endDate());
        subscription.setJobPostLimit(request.jobPostLimit());
        subscription.setApplicantLimit(request.applicantLimit());
        subscription.setApplicationLimit(request.applicationLimit());
        subscription.setResumeScoreLimit(request.resumeScoreLimit());
        if (request.applicationUsed() != null) {
            subscription.setApplicationUsed(request.applicationUsed());
        } else if (subscription.getApplicationUsed() == null) {
            subscription.setApplicationUsed(0);
        }
        if (request.resumeScoreUsed() != null) {
            subscription.setResumeScoreUsed(request.resumeScoreUsed());
        } else if (subscription.getResumeScoreUsed() == null) {
            subscription.setResumeScoreUsed(0);
        }
        subscription.setPaymentReference(request.paymentReference());
        subscription.setActive(request.active() == null || request.active());

        return subscriptionMapper.toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    public SubscriptionResponse getByUserId(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for user: " + userId));
        return subscriptionMapper.toResponse(subscription);
    }
}
