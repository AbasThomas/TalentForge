package com.TalentForge.talentforge.applicant.dto;

import com.TalentForge.talentforge.applicant.entity.ResumeScoreTaskStatus;

import java.time.LocalDateTime;

public record ResumeScoreTaskSubmitResponse(
        Long taskId,
        ResumeScoreTaskStatus status,
        String message,
        LocalDateTime createdAt
) {
}
