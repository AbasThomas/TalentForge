package com.TalentForge.talentforge.applicant.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicantResumeScoreResponse(
        Double score,
        String reason,
        String matchingKeywords,
        Integer parsedCharacters,
        String source,
        Boolean usedApplicantProfile,
        LocalDateTime analyzedAt,
        List<String> processingLogs
) {
}
