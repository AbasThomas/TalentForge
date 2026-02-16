package com.TalentForge.talentforge.subscription.service;

import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.application.repository.ApplicationRepository;
import com.TalentForge.talentforge.job.entity.JobStatus;
import com.TalentForge.talentforge.job.repository.JobRepository;
import com.TalentForge.talentforge.subscription.dto.SubscriptionRequest;
import com.TalentForge.talentforge.subscription.dto.SubscriptionResponse;
import com.TalentForge.talentforge.subscription.dto.SubscriptionUsageResponse;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import com.TalentForge.talentforge.subscription.mapper.SubscriptionMapper;
import com.TalentForge.talentforge.subscription.repository.SubscriptionRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
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

    @Override
    public SubscriptionUsageResponse getUsageByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for user: " + user.getId()));

        Integer jobPostUsed = null;
        Integer applicantUsed = null;
        if (user.getRole() == UserRole.RECRUITER || user.getRole() == UserRole.ADMIN) {
            jobPostUsed = toInt(jobRepository.countByRecruiterIdAndStatusIn(
                    user.getId(),
                    List.of(JobStatus.DRAFT, JobStatus.OPEN)
            ));
            applicantUsed = toInt(applicationRepository.countByJobRecruiterId(user.getId()));
        }

        Integer applicationUsed = subscription.getApplicationUsed() == null ? null : Math.max(0, subscription.getApplicationUsed());
        Integer resumeScoreUsed = subscription.getResumeScoreUsed() == null ? null : Math.max(0, subscription.getResumeScoreUsed());

        return new SubscriptionUsageResponse(
                subscription.getPlanType(),
                subscription.isActive(),
                subscription.getEndDate(),
                subscription.getJobPostLimit(),
                jobPostUsed,
                remaining(subscription.getJobPostLimit(), jobPostUsed),
                subscription.getApplicantLimit(),
                applicantUsed,
                remaining(subscription.getApplicantLimit(), applicantUsed),
                subscription.getApplicationLimit(),
                applicationUsed,
                remaining(subscription.getApplicationLimit(), applicationUsed),
                subscription.getResumeScoreLimit(),
                resumeScoreUsed,
                remaining(subscription.getResumeScoreLimit(), resumeScoreUsed),
                subscription.getPaymentReference()
        );
    }

    private Integer remaining(Integer limit, Integer used) {
        if (limit == null || limit <= 0 || used == null) {
            return null;
        }
        return Math.max(0, limit - used);
    }

    private Integer toInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }
}
