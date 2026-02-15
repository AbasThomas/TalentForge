package com.TalentForge.talentforge.applicant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicantResumeScoreRequest {
    private String targetRole;
    private String jobDescription;
    private String requirements;
    private String coverLetter;
}
