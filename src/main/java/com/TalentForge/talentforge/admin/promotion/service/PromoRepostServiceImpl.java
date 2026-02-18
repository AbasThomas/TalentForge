package com.TalentForge.talentforge.admin.promotion.service;

import com.TalentForge.talentforge.admin.promotion.dto.PromoRepostResponse;
import com.TalentForge.talentforge.admin.promotion.entity.PromoRepost;
import com.TalentForge.talentforge.admin.promotion.entity.PromoRepostStatus;
import com.TalentForge.talentforge.admin.promotion.repository.PromoRepostRepository;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoRepostServiceImpl implements PromoRepostService {

    private final PromoRepostRepository promoRepostRepository;
    private final JobRepository jobRepository;

    @Override
    public List<PromoRepostResponse> getAll() {
        return promoRepostRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PromoRepostResponse setConsent(Long jobId, boolean consentGiven) {
        PromoRepost repost = getOrCreate(jobId);
        repost.setConsentGiven(consentGiven);
        if (!consentGiven && repost.getStatus() == PromoRepostStatus.APPROVED) {
            repost.setStatus(PromoRepostStatus.DECLINED);
            repost.setApprovedAt(null);
        }
        return toResponse(promoRepostRepository.save(repost));
    }

    @Override
    @Transactional
    public PromoRepostResponse setStatus(Long jobId, PromoRepostStatus status) {
        PromoRepost repost = getOrCreate(jobId);
        repost.setStatus(status);
        if (status == PromoRepostStatus.APPROVED) {
            repost.setApprovedAt(LocalDateTime.now());
        } else if (status == PromoRepostStatus.DECLINED) {
            repost.setApprovedAt(null);
        }
        return toResponse(promoRepostRepository.save(repost));
    }

    private PromoRepost getOrCreate(Long jobId) {
        PromoRepost existing = promoRepostRepository.findByJobId(jobId).orElse(null);
        if (existing != null) {
            return existing;
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        return PromoRepost.builder()
                .job(job)
                .consentGiven(false)
                .status(PromoRepostStatus.PENDING)
                .build();
    }

    private PromoRepostResponse toResponse(PromoRepost repost) {
        return new PromoRepostResponse(
                repost.getId(),
                repost.getJob().getId(),
                repost.isConsentGiven(),
                repost.getStatus(),
                repost.getApprovedAt(),
                repost.getCreatedAt(),
                repost.getUpdatedAt()
        );
    }
}
