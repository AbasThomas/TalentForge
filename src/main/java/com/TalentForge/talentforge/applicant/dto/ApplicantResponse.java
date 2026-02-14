package com.TalentForge.talentforge.applicant.dto;

import java.time.LocalDateTime;

public record ApplicantResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String location,
        String linkedinUrl,
        String portfolioUrl,
        String summary,
        String skills,
        Integer yearsOfExperience,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
