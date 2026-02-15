package com.TalentForge.talentforge.application.dto;

import com.TalentForge.talentforge.application.entity.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        Long applicantId,
        String applicantName,
        ApplicationStatus status,
        String resumeFileName,
        String resumeFilePath,
        String resumeFileType,
        Double aiScore,
        String aiScoreReason,
        String matchingKeywords,
        List<String> processingLogs,
        String coverLetter,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt,
        LocalDateTime reviewedAt,
        LocalDateTime interviewedAt
) {
}
