package com.TalentForge.talentforge.job.service;

import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.job.dto.JobRequest;
import com.TalentForge.talentforge.job.dto.JobResponse;
import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.job.entity.JobStatus;
import com.TalentForge.talentforge.job.mapper.JobMapper;
import com.TalentForge.talentforge.job.repository.JobRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final AiAssistantService aiAssistantService;

    @Override
    public JobResponse create(JobRequest request) {
        User recruiter = userRepository.findById(request.recruiterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + request.recruiterId()));

        Job job = Job.builder()
                .title(request.title())
                .description(request.description())
                .requirements(request.requirements())
                .location(request.location())
                .department(request.department())
                .salaryRange(request.salaryRange())
                .jobType(request.jobType())
                .experienceLevel(request.experienceLevel())
                .status(request.status() == null ? JobStatus.DRAFT : request.status())
                .recruiter(recruiter)
                .closingDate(request.closingDate())
                .build();

        job.setBiasCheckResult(aiAssistantService.checkJobBias(
                job.getTitle(),
                job.getDescription(),
                job.getRequirements()
        ));

        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Override
    public JobResponse update(Long id, JobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));

        User recruiter = userRepository.findById(request.recruiterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + request.recruiterId()));

        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setLocation(request.location());
        job.setDepartment(request.department());
        job.setSalaryRange(request.salaryRange());
        job.setJobType(request.jobType());
        job.setExperienceLevel(request.experienceLevel());
        job.setStatus(request.status() == null ? job.getStatus() : request.status());
        job.setRecruiter(recruiter);
        job.setClosingDate(request.closingDate());
        job.setBiasCheckResult(aiAssistantService.checkJobBias(
                job.getTitle(),
                job.getDescription(),
                job.getRequirements()
        ));

        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Override
    public JobResponse getById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
        return jobMapper.toResponse(job);
    }

    @Override
    public List<JobResponse> getAll() {
        return jobRepository.findAll().stream().map(jobMapper::toResponse).toList();
    }

    @Override
    public List<JobResponse> getByRecruiter(Long recruiterId) {
        return jobRepository.findByRecruiterId(recruiterId).stream().map(jobMapper::toResponse).toList();
    }

    public List<JobResponse> getOpenJobs() {
        return jobRepository.findByStatus(JobStatus.OPEN).stream().map(jobMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
        jobRepository.delete(job);
    }
}
