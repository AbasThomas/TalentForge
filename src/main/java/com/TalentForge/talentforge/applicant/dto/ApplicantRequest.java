package com.TalentForge.talentforge.applicant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApplicantRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        String location,
        String linkedinUrl,
        String portfolioUrl,
        String summary,
        String skills,
        Integer yearsOfExperience
) {
}
