package com.TalentForge.talentforge.job.service;

import com.TalentForge.talentforge.job.dto.JobRequest;
import com.TalentForge.talentforge.job.dto.JobResponse;

import java.util.List;

public interface JobService {
    JobResponse create(JobRequest request);

    JobResponse update(Long id, JobRequest request);

    JobResponse getById(Long id);

    List<JobResponse> getAll();

    List<JobResponse> getByRecruiter(Long recruiterId);

    void delete(Long id);
}
