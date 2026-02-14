package com.TalentForge.talentforge.job.dto;

import com.TalentForge.talentforge.job.entity.ExperienceLevel;
import com.TalentForge.talentforge.job.entity.JobStatus;
import com.TalentForge.talentforge.job.entity.JobType;

import java.time.LocalDateTime;

public record JobResponse(
        Long id,
        String title,
        String description,
        String requirements,
        String location,
        String department,
        String salaryRange,
        JobType jobType,
        ExperienceLevel experienceLevel,
        JobStatus status,
        Long recruiterId,
        String recruiterName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closingDate,
        String biasCheckResult
) {
}
