package com.TalentForge.talentforge.application.dto;

import com.TalentForge.talentforge.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationCreateRequest {
    @NotNull
    private Long jobId;

    @NotNull
    private Long applicantId;

    private ApplicationStatus status;

    private String coverLetter;
}
