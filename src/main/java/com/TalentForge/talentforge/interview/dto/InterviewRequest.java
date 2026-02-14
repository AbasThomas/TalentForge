package com.TalentForge.talentforge.interview.dto;

import com.TalentForge.talentforge.interview.entity.InterviewStatus;
import com.TalentForge.talentforge.interview.entity.InterviewType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InterviewRequest(
        @NotNull Long applicationId,
        LocalDateTime scheduledAt,
        InterviewType interviewType,
        String meetingLink,
        InterviewStatus status,
        String feedback
) {
}
