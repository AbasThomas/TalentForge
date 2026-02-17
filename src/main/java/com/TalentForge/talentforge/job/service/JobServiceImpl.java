package com.TalentForge.talentforge.job.service;

import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.job.dto.JobRequest;
import com.TalentForge.talentforge.job.dto.JobResponse;
import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.job.entity.JobStatus;
import com.TalentForge.talentforge.job.mapper.JobMapper;
import com.TalentForge.talentforge.job.repository.JobRepository;
import com.TalentForge.talentforge.subscription.service.SubscriptionLimitService;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final AiAssistantService aiAssistantService;
    private final SubscriptionLimitService subscriptionLimitService;

    @Override
    public JobResponse create(JobRequest request) {
        User actor = getAuthenticatedUser();
        assertOwnershipOrAdmin(actor, request.recruiterId());

        User recruiter = userRepository.findById(request.recruiterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + request.recruiterId()));
        if (!recruiter.isActive()) {
            throw new BadRequestException("Inactive users cannot create jobs");
        }
        subscriptionLimitService.ensureRecruiterCanPostJob(recruiter.getId());

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
        User actor = getAuthenticatedUser();
        assertOwnershipOrAdmin(actor, request.recruiterId());

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

        User actor = getAuthenticatedUser();
        assertOwnershipOrAdmin(actor, job.getRecruiter().getId());

        jobRepository.delete(job);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    private void assertOwnershipOrAdmin(User actor, Long ownerId) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!actor.getId().equals(ownerId)) {
            throw new AccessDeniedException("You can only manage jobs for your own account");
        }
    }
}
