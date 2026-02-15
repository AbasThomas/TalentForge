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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public ApplicationResponse submit(ApplicationCreateRequest request, MultipartFile resumeFile) {
        List<String> processingLogs = new ArrayList<>();
        processingLogs.add(stageLog("RECEIVED", "Application payload received"));

        if (request.getJobId() == null || request.getApplicantId() == null) {
            processingLogs.add(stageLog("VALIDATION_FAILED", "jobId and applicantId are required"));
            throw new BadRequestException("jobId and applicantId are required");
        }

        if (applicationRepository.existsByJobIdAndApplicantId(request.getJobId(), request.getApplicantId())) {
            processingLogs.add(stageLog("VALIDATION_FAILED", "Duplicate application for same job and applicant"));
            throw new BadRequestException("Applicant already applied to this job");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.getJobId()));
        processingLogs.add(stageLog("JOB_FETCHED", "Job loaded: " + job.getTitle()));

        Applicant applicant = applicantRepository.findById(request.getApplicantId())
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + request.getApplicantId()));
        processingLogs.add(stageLog("APPLICANT_FETCHED", "Applicant loaded: " + applicant.getEmail()));

        Application application = Application.builder()
                .job(job)
                .applicant(applicant)
                .status(request.getStatus() == null ? ApplicationStatus.APPLIED : request.getStatus())
                .coverLetter(request.getCoverLetter())
                .processingLogs(processingLogs)
                .build();

        if (resumeFile != null && !resumeFile.isEmpty()) {
            try {
                processingLogs.add(stageLog("RESUME_RECEIVED", "File accepted: " + resumeFile.getOriginalFilename()));
                String resumePath = resumeStorageService.store(resumeFile);
                processingLogs.add(stageLog("RESUME_STORED", "Stored at: " + resumePath));
                String resumeText = resumeParserService.extractText(resumeFile);
                processingLogs.add(stageLog("RESUME_PARSED", "Tika extracted characters: " + resumeText.length()));

                application.setResumeFileName(resumeFile.getOriginalFilename());
                application.setResumeFilePath(resumePath);
                application.setResumeFileType(resumeFile.getContentType());
                application.setResumeText(resumeText);

                String jobText = (job.getTitle() == null ? "" : job.getTitle()) + "\n"
                        + (job.getDescription() == null ? "" : job.getDescription()) + "\n"
                        + (job.getRequirements() == null ? "" : job.getRequirements());
                processingLogs.add(stageLog("PROMPT_BUILT", "Prompt assembled from job description and parsed resume"));

                AiResumeScoreResult scoreResult = aiAssistantService.scoreResume(jobText, resumeText);
                application.setAiScore(scoreResult.score());
                application.setAiScoreReason(scoreResult.reason());
                application.setMatchingKeywords(scoreResult.matchingKeywords());
                processingLogs.add(stageLog("AI_SCORED", "AI score computed: " + scoreResult.score()));

                Map<String, Object> aiAnalysis = new LinkedHashMap<>();
                aiAnalysis.put("score", scoreResult.score());
                aiAnalysis.put("skills", scoreResult.matchingKeywords());
                aiAnalysis.put("reasoning", scoreResult.reason());
                aiAnalysis.put("processedAt", LocalDateTime.now().toString());
                aiAnalysis.put("jobTitle", job.getTitle());
                aiAnalysis.put("resumeChars", resumeText.length());
                applicant.setAiScore(scoreResult.score());
                applicant.setAiAnalysis(aiAnalysis);
                applicantRepository.save(applicant);
                processingLogs.add(stageLog("APPLICANT_UPDATED", "Applicant ai_score and ai_analysis updated"));
            } catch (IOException ex) {
                processingLogs.add(stageLog("PARSING_FAILED", "Resume processing failed: " + ex.getMessage()));
                throw new BadRequestException("Could not process resume file: " + ex.getMessage());
            }
        } else {
            processingLogs.add(stageLog("NO_RESUME", "Resume file not provided"));
        }

        processingLogs.add(stageLog("SAVING", "Persisting application with AI analysis"));
        application.setProcessingLogs(processingLogs);
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

    private String stageLog(String stage, String detail) {
        return LOG_TIME_FORMAT.format(LocalDateTime.now()) + " | " + stage + " | " + detail;
    }
}
