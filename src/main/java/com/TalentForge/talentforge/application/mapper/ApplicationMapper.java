package com.TalentForge.talentforge.application.mapper;

import com.TalentForge.talentforge.application.dto.ApplicationResponse;
import com.TalentForge.talentforge.application.entity.Application;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getApplicant().getId(),
                application.getApplicant().getFullName(),
                application.getStatus(),
                application.getResumeFileName(),
                application.getResumeFilePath(),
                application.getResumeFileType(),
                application.getAiScore(),
                application.getAiScoreReason(),
                application.getMatchingKeywords(),
                application.getProcessingLogs(),
                application.getCoverLetter(),
                application.getAppliedAt(),
                application.getUpdatedAt(),
                application.getReviewedAt(),
                application.getInterviewedAt()
        );
    }
}
