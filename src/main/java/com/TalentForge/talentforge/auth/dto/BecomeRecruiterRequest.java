package com.TalentForge.talentforge.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BecomeRecruiterRequest(
        @NotBlank @Size(max = 255) String company,
        @NotBlank @Size(max = 40) String phone,
        @NotBlank @Size(max = 255) String companyWebsite,
        @NotBlank @Size(max = 120) String recruiterJobTitle,
        @NotBlank @Size(max = 80) String recruiterTeamSize,
        @NotNull @AssertTrue(message = "Recruiter terms must be accepted") Boolean recruiterTermsAccepted,
        @NotNull @AssertTrue(message = "Data handling consent must be accepted") Boolean recruiterDataConsentAccepted,
        @NotNull @AssertTrue(message = "Hiring authority confirmation is required") Boolean recruiterAuthorityConfirmed
) {
}
