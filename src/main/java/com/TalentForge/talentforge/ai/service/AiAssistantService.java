package com.TalentForge.talentforge.ai.service;

import com.TalentForge.talentforge.ai.dto.AiResumeScoreResult;

public interface AiAssistantService {
    String checkJobBias(String title, String description, String requirements);

    AiResumeScoreResult scoreResume(String jobText, String resumeText);

    String generateChatReply(String message);
}
