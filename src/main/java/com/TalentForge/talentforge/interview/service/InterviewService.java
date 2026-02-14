package com.TalentForge.talentforge.interview.service;

import com.TalentForge.talentforge.interview.dto.InterviewRequest;
import com.TalentForge.talentforge.interview.dto.InterviewResponse;

import java.util.List;

public interface InterviewService {
    InterviewResponse create(InterviewRequest request);

    List<InterviewResponse> getByApplication(Long applicationId);
}
