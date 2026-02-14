package com.TalentForge.talentforge.job.mapper;

import com.TalentForge.talentforge.job.dto.JobResponse;
import com.TalentForge.talentforge.job.entity.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getRequirements(),
                job.getLocation(),
                job.getDepartment(),
                job.getSalaryRange(),
                job.getJobType(),
                job.getExperienceLevel(),
                job.getStatus(),
                job.getRecruiter() == null ? null : job.getRecruiter().getId(),
                job.getRecruiter() == null ? null : job.getRecruiter().getFullName(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getClosingDate(),
                job.getBiasCheckResult()
        );
    }
}
