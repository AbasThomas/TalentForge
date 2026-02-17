package com.TalentForge.talentforge.applicant.dto;

import java.time.LocalDateTime;

public record ResumeScoreHistoryItemResponse(
        Long id,
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
