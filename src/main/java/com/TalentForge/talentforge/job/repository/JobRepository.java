package com.TalentForge.talentforge.job.repository;

import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.job.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiterId(Long recruiterId);

    List<Job> findByStatus(JobStatus status);
}
