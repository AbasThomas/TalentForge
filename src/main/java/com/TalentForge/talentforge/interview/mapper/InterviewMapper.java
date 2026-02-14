package com.TalentForge.talentforge.interview.mapper;

import com.TalentForge.talentforge.interview.dto.InterviewResponse;
import com.TalentForge.talentforge.interview.entity.Interview;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewResponse toResponse(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getApplication().getId(),
                interview.getScheduledAt(),
                interview.getInterviewType(),
                interview.getMeetingLink(),
                interview.getStatus(),
                interview.getFeedback()
        );
    }
}
