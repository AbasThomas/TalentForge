package com.TalentForge.talentforge.interview.dto;

import com.TalentForge.talentforge.interview.entity.InterviewStatus;
import com.TalentForge.talentforge.interview.entity.InterviewType;

import java.time.LocalDateTime;

public record InterviewResponse(
        Long id,
        Long applicationId,
        LocalDateTime scheduledAt,
        InterviewType interviewType,
        String meetingLink,
        InterviewStatus status,
        String feedback
) {
}
