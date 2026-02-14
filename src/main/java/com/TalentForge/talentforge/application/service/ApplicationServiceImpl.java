package com.TalentForge.talentforge.application.service;

import com.TalentForge.talentforge.ai.dto.AiResumeScoreResult;
import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.ai.service.ResumeParserService;
import com.TalentForge.talentforge.applicant.entity.Applicant;
import com.TalentForge.talentforge.applicant.repository.ApplicantRepository;
import com.TalentForge.talentforge.application.dto.ApplicationCreateRequest;
import com.TalentForge.talentforge.application.dto.ApplicationResponse;
import com.TalentForge.talentforge.application.entity.Application;
import com.TalentForge.talentforge.application.entity.ApplicationStatus;
import com.TalentForge.talentforge.application.mapper.ApplicationMapper;
import com.TalentForge.talentforge.application.repository.ApplicationRepository;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationMapper applicationMapper;
    private final ResumeStorageService resumeStorageService;
    private final ResumeParserService resumeParserService;
    private final AiAssistantService aiAssistantService;

    @Override
    public ApplicationResponse submit(ApplicationCreateRequest request, MultipartFile resumeFile) {
        if (request.getJobId() == null || request.getApplicantId() == null) {
            throw new BadRequestException("jobId and applicantId are required");
        }

        if (applicationRepository.existsByJobIdAndApplicantId(request.getJobId(), request.getApplicantId())) {
            throw new BadRequestException("Applicant already applied to this job");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.getJobId()));

        Applicant applicant = applicantRepository.findById(request.getApplicantId())
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + request.getApplicantId()));

        Application application = Application.builder()
                .job(job)
                .applicant(applicant)
                .status(request.getStatus() == null ? ApplicationStatus.APPLIED : request.getStatus())
                .coverLetter(request.getCoverLetter())
                .build();

        if (resumeFile != null && !resumeFile.isEmpty()) {
            try {
                String resumePath = resumeStorageService.store(resumeFile);
                String resumeText = resumeParserService.extractText(resumeFile);

                application.setResumeFileName(resumeFile.getOriginalFilename());
                application.setResumeFilePath(resumePath);
                application.setResumeFileType(resumeFile.getContentType());
                application.setResumeText(resumeText);

                String jobText = (job.getTitle() == null ? "" : job.getTitle()) + "\n"
                        + (job.getDescription() == null ? "" : job.getDescription()) + "\n"
                        + (job.getRequirements() == null ? "" : job.getRequirements());

                AiResumeScoreResult scoreResult = aiAssistantService.scoreResume(jobText, resumeText);
                application.setAiScore(scoreResult.score());
                application.setAiScoreReason(scoreResult.reason());
                application.setMatchingKeywords(scoreResult.matchingKeywords());
            } catch (IOException ex) {
                throw new BadRequestException("Could not process resume file: " + ex.getMessage());
            }
        }

        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponse updateStatus(Long id, ApplicationStatus status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));

        application.setStatus(status);
        if (status == ApplicationStatus.REVIEWING) {
            application.setReviewedAt(LocalDateTime.now());
        }
        if (status == ApplicationStatus.INTERVIEWED) {
            application.setInterviewedAt(LocalDateTime.now());
        }

        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponse getById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        return applicationMapper.toResponse(application);
    }

    @Override
    public List<ApplicationResponse> getByJobId(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream().map(applicationMapper::toResponse).toList();
    }

    @Override
    public List<ApplicationResponse> getByApplicantId(Long applicantId) {
        return applicationRepository.findByApplicantId(applicantId).stream().map(applicationMapper::toResponse).toList();
    }
}
