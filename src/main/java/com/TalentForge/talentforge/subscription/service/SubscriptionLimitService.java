package com.TalentForge.talentforge.subscription.service;

import com.TalentForge.talentforge.application.repository.ApplicationRepository;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.job.entity.JobStatus;
import com.TalentForge.talentforge.job.repository.JobRepository;
import com.TalentForge.talentforge.subscription.entity.Subscription;
import com.TalentForge.talentforge.subscription.repository.SubscriptionRepository;
import com.TalentForge.talentforge.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionLimitService {

    private final SubscriptionRepository subscriptionRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public void ensureRecruiterCanPostJob(Long recruiterId) {
        Subscription subscription = getSubscriptionOrThrow(recruiterId);
        Integer jobPostLimit = subscription.getJobPostLimit();
        if (jobPostLimit == null || jobPostLimit <= 0) {
            return;
        }

        long activeJobs = jobRepository.countByRecruiterIdAndStatusIn(
                recruiterId,
                List.of(JobStatus.DRAFT, JobStatus.OPEN)
        );
        if (activeJobs >= jobPostLimit) {
            throw new BadRequestException("Plan limit reached: maximum active jobs is " + jobPostLimit);
        }
    }

    @Transactional(readOnly = true)
    public void ensureRecruiterCanReceiveApplicant(Long recruiterId) {
        Subscription subscription = getSubscriptionOrThrow(recruiterId);
        Integer applicantLimit = subscription.getApplicantLimit();
        if (applicantLimit == null || applicantLimit <= 0) {
            return;
        }

        long currentApplicants = applicationRepository.countByJobRecruiterId(recruiterId);
        if (currentApplicants >= applicantLimit) {
            throw new BadRequestException("Plan limit reached: maximum applicant pipeline is " + applicantLimit);
        }
    }

    @Transactional(readOnly = true)
    public void ensureCandidateCanSubmitApplication(User candidate) {
        Subscription subscription = getSubscriptionOrThrow(candidate.getId());
        Integer applicationLimit = subscription.getApplicationLimit();
        if (applicationLimit == null || applicationLimit <= 0) {
            return;
        }

        Integer used = safeCount(subscription.getApplicationUsed());
        if (used >= applicationLimit) {
            throw new BadRequestException("Plan limit reached: maximum applications is " + applicationLimit);
        }
    }

    public void incrementCandidateApplicationUsage(User candidate) {
        Subscription subscription = getSubscriptionOrThrow(candidate.getId());
        subscription.setApplicationUsed(safeCount(subscription.getApplicationUsed()) + 1);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public void ensureCandidateCanScoreResume(User candidate) {
        Subscription subscription = getSubscriptionOrThrow(candidate.getId());
        Integer resumeScoreLimit = subscription.getResumeScoreLimit();
        if (resumeScoreLimit == null || resumeScoreLimit <= 0) {
            return;
        }

        Integer used = safeCount(subscription.getResumeScoreUsed());
        if (used >= resumeScoreLimit) {
            throw new BadRequestException("Plan limit reached: maximum resume AI scorings is " + resumeScoreLimit);
        }
    }

    public void incrementCandidateResumeScoreUsage(User candidate) {
        Subscription subscription = getSubscriptionOrThrow(candidate.getId());
        subscription.setResumeScoreUsed(safeCount(subscription.getResumeScoreUsed()) + 1);
        subscriptionRepository.save(subscription);
    }

    private Subscription getSubscriptionOrThrow(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for user: " + userId));
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
