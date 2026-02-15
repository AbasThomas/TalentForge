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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_CANDIDATE_TEXT_CHARS = 14000;

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationMapper applicationMapper;
    private final ResumeStorageService resumeStorageService;
    private final ResumeParserService resumeParserService;
    private final AiAssistantService aiAssistantService;

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

        String parsedResumeText = "";
        if (resumeFile != null && !resumeFile.isEmpty()) {
            try {
                processingLogs.add(stageLog("RESUME_RECEIVED", "File accepted: " + resumeFile.getOriginalFilename()));
                String resumePath = resumeStorageService.store(resumeFile);
                processingLogs.add(stageLog("RESUME_STORED", "Stored at: " + resumePath));

                parsedResumeText = truncate(resumeParserService.extractText(resumeFile), MAX_RESUME_CHARS);
                processingLogs.add(stageLog("RESUME_PARSED", "Extracted characters: " + parsedResumeText.length()));

                application.setResumeFileName(resumeFile.getOriginalFilename());
                application.setResumeFilePath(resumePath);
                application.setResumeFileType(resumeFile.getContentType());
                application.setResumeText(parsedResumeText);
            } catch (IOException ex) {
                processingLogs.add(stageLog("RESUME_PROCESSING_FAILED", "Resume parsing failed, using profile fallback"));
                log.warn("Resume parsing failed for applicantId={} jobId={}", applicant.getId(), job.getId(), ex);
            }
        } else {
            processingLogs.add(stageLog("NO_RESUME", "Resume file not provided, using profile text for scoring"));
        }

        String jobText = buildJobText(job);
        String candidateText = buildCandidateText(applicant, request.getCoverLetter(), parsedResumeText);
        runAiScoring(application, applicant, job, jobText, candidateText, processingLogs, "SUBMIT");

        processingLogs.add(stageLog("SAVING", "Persisting application"));
        application.setProcessingLogs(processingLogs);
        return applicationMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponse rescore(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));

        List<String> processingLogs = new ArrayList<>();
        if (application.getProcessingLogs() != null) {
            processingLogs.addAll(application.getProcessingLogs());
        }

        processingLogs.add(stageLog("RESCORE_REQUESTED", "Manual AI scoring triggered"));
        String jobText = buildJobText(application.getJob());
        String candidateText = buildCandidateText(
                application.getApplicant(),
                application.getCoverLetter(),
                truncate(application.getResumeText(), MAX_RESUME_CHARS)
        );

        runAiScoring(application, application.getApplicant(), application.getJob(), jobText, candidateText, processingLogs, "RESCORE");

        processingLogs.add(stageLog("RESCORE_COMPLETED", "Application AI score refreshed"));
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

    private void runAiScoring(
            Application application,
            Applicant applicant,
            Job job,
            String jobText,
            String candidateText,
            List<String> processingLogs,
            String trigger
    ) {
        if (candidateText.isBlank()) {
            processingLogs.add(stageLog("AI_SKIPPED", "No candidate text available for AI scoring"));
            application.setAiScore(0.0);
            application.setAiScoreReason("No resume or profile text found to evaluate.");
            application.setMatchingKeywords("");
            return;
        }

        try {
            processingLogs.add(stageLog("AI_SCORING", "Computing Talentforge AI score (trigger=" + trigger + ")"));
            AiResumeScoreResult scoreResult = aiAssistantService.scoreResume(jobText, truncate(candidateText, MAX_CANDIDATE_TEXT_CHARS));

            double boundedScore = Math.max(0, Math.min(100, scoreResult.score()));
            String reason = normalizeReason(scoreResult.reason());
            String keywords = normalizeKeywords(scoreResult.matchingKeywords());

            application.setAiScore(boundedScore);
            application.setAiScoreReason(reason);
            application.setMatchingKeywords(keywords);
            processingLogs.add(stageLog("AI_SCORED", "AI score computed: " + boundedScore));

            Map<String, Object> aiAnalysis = new LinkedHashMap<>();
            aiAnalysis.put("provider", "Talentforge AI");
            aiAnalysis.put("score", boundedScore);
            aiAnalysis.put("skills", keywords);
            aiAnalysis.put("reasoning", reason);
            aiAnalysis.put("processedAt", LocalDateTime.now().toString());
            aiAnalysis.put("jobTitle", job.getTitle());
            aiAnalysis.put("trigger", trigger);
            aiAnalysis.put("candidateTextChars", candidateText.length());

            applicant.setAiScore(boundedScore);
            applicant.setAiAnalysis(aiAnalysis);
            applicantRepository.save(applicant);
            processingLogs.add(stageLog("APPLICANT_UPDATED", "Applicant ai_score and ai_analysis updated"));
        } catch (Exception ex) {
            processingLogs.add(stageLog("AI_FAILED", "AI scoring failed, deterministic fallback applied"));
            log.warn("AI scoring failed for applicationId={} applicantId={}", application.getId(), applicant.getId(), ex);

            application.setAiScore(0.0);
            application.setAiScoreReason("Talentforge AI scoring is temporarily unavailable. Retry from recruiter application detail.");
            application.setMatchingKeywords("");

            Map<String, Object> aiAnalysis = new LinkedHashMap<>();
            aiAnalysis.put("provider", "Talentforge AI");
            aiAnalysis.put("score", 0.0);
            aiAnalysis.put("reasoning", "AI scoring failed before response generation.");
            aiAnalysis.put("processedAt", LocalDateTime.now().toString());
            aiAnalysis.put("trigger", trigger);
            aiAnalysis.put("failed", true);
            applicant.setAiScore(0.0);
            applicant.setAiAnalysis(aiAnalysis);
            applicantRepository.save(applicant);
        }
    }

    private String buildJobText(Job job) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Title", job.getTitle());
        appendSection(builder, "Description", job.getDescription());
        appendSection(builder, "Requirements", job.getRequirements());
        appendSection(builder, "Department", job.getDepartment());
        appendSection(builder, "Experience Level", job.getExperienceLevel() == null ? null : job.getExperienceLevel().name());
        appendSection(builder, "Job Type", job.getJobType() == null ? null : job.getJobType().name());
        return truncate(builder.toString(), MAX_CANDIDATE_TEXT_CHARS);
    }

    private String buildCandidateText(Applicant applicant, String coverLetter, String resumeText) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Resume", resumeText);
        appendSection(builder, "Cover Letter", coverLetter);
        appendSection(builder, "Applicant Name", applicant.getFullName());
        appendSection(builder, "Summary", applicant.getSummary());
        appendSection(builder, "Skills", applicant.getSkills());
        appendSection(builder, "Experience", applicant.getYearsOfExperience() == null ? null : applicant.getYearsOfExperience() + " years");
        appendSection(builder, "Location", applicant.getLocation());
        appendSection(builder, "LinkedIn", applicant.getLinkedinUrl());
        appendSection(builder, "Portfolio", applicant.getPortfolioUrl());

        return truncate(builder.toString(), MAX_CANDIDATE_TEXT_CHARS);
    }

    private void appendSection(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append(label).append(':').append('\n').append(value.trim());
    }

    private String normalizeKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return "";
        }

        return List.of(keywords.split(","))
                .stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .distinct()
                .limit(20)
                .collect(Collectors.joining(", "));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Talentforge AI scored this application, but no explanation text was returned by the model.";
        }
        return truncate(reason.trim(), 800);
    }

    private String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }

        String compact = value.replace("\u0000", "").trim();
        if (compact.length() <= maxChars) {
            return compact;
        }

        return compact.substring(0, maxChars);
    }

    private String stageLog(String stage, String detail) {
        return LOG_TIME_FORMAT.format(LocalDateTime.now()) + " | " + stage + " | " + detail;
    }
}
