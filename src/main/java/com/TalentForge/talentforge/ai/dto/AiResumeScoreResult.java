package com.TalentForge.talentforge.ai.dto;

public record AiResumeScoreResult(
        double score,
        String reason,
        String matchingKeywords
) {
}
