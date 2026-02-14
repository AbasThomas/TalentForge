package com.TalentForge.talentforge.job.dto;

import com.TalentForge.talentforge.job.entity.ExperienceLevel;
import com.TalentForge.talentforge.job.entity.JobStatus;
import com.TalentForge.talentforge.job.entity.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record JobRequest(
        @NotBlank String title,
        @NotBlank String description,
        String requirements,
        String location,
        String department,
        String salaryRange,
        JobType jobType,
        ExperienceLevel experienceLevel,
        JobStatus status,
        @NotNull Long recruiterId,
        LocalDateTime closingDate
) {
}
