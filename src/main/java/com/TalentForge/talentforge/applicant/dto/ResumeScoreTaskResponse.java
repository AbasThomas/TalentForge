package com.TalentForge.talentforge.applicant.dto;

import com.TalentForge.talentforge.applicant.entity.ResumeScoreTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ResumeScoreTaskResponse(
        Long id,
        ResumeScoreTaskStatus status,
        String fileName,
        String fileContentType,
        String targetRole,
        Double score,
        String reason,
        String matchingKeywords,
        Integer parsedCharacters,
        String source,
        Boolean usedApplicantProfile,
        List<String> processingLogs,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
