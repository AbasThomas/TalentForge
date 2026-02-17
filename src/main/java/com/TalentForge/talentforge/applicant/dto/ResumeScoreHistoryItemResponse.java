package com.TalentForge.talentforge.applicant.dto;

import com.TalentForge.talentforge.applicant.entity.ResumeScoreTaskStatus;

import java.time.LocalDateTime;

public record ResumeScoreHistoryItemResponse(
        Long id,
        Long taskId,
        ResumeScoreTaskStatus taskStatus,
        Double score,
        String reason,
        String matchingKeywords,
        Integer parsedCharacters,
        String source,
        Boolean usedApplicantProfile,
        String fileName,
        String targetRole,
        LocalDateTime createdAt
) {
}
