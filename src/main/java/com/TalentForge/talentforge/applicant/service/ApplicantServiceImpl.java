package com.TalentForge.talentforge.applicant.service;

import com.TalentForge.talentforge.ai.dto.AiResumeScoreResult;
import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.ai.service.ResumeParserService;
import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreResponse;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.entity.Applicant;
import com.TalentForge.talentforge.applicant.mapper.ApplicantMapper;
import com.TalentForge.talentforge.applicant.repository.ApplicantRepository;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_CONTEXT_CHARS = 14000;

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;
    private final ResumeParserService resumeParserService;
    private final AiAssistantService aiAssistantService;

    @Override
    public ApplicantResponse create(ApplicantRequest request) {
        if (applicantRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Applicant email already exists");
        }

        Applicant applicant = Applicant.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .location(request.location())
                .linkedinUrl(request.linkedinUrl())
                .portfolioUrl(request.portfolioUrl())
                .summary(request.summary())
                .skills(request.skills())
                .yearsOfExperience(request.yearsOfExperience())
                .build();

        return applicantMapper.toResponse(applicantRepository.save(applicant));
    }

    @Override
    public ApplicantResponse update(Long id, ApplicantRequest request) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + id));

        applicant.setFullName(request.fullName());
        applicant.setPhone(request.phone());
        applicant.setLocation(request.location());
        applicant.setLinkedinUrl(request.linkedinUrl());
        applicant.setPortfolioUrl(request.portfolioUrl());
        applicant.setSummary(request.summary());
        applicant.setSkills(request.skills());
        applicant.setYearsOfExperience(request.yearsOfExperience());

        return applicantMapper.toResponse(applicantRepository.save(applicant));
    }

    @Override
    public ApplicantResponse getById(Long id) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + id));
        return applicantMapper.toResponse(applicant);
    }

    @Override
    public List<ApplicantResponse> getAll() {
        return applicantRepository.findAll().stream().map(applicantMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + id));
        applicantRepository.delete(applicant);
    }

    @Override
    public ApplicantResumeScoreResponse scoreResume(String userEmail, ApplicantResumeScoreRequest request, MultipartFile resumeFile) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("Authenticated user email is required");
        }
        if (resumeFile == null || resumeFile.isEmpty()) {
            throw new BadRequestException("resumeFile is required");
        }

        List<String> processingLogs = new ArrayList<>();
        processingLogs.add(stageLog("REQUEST_RECEIVED", "Talentforge candidate resume score request received"));

        String resumeText;
        try {
            resumeText = truncate(resumeParserService.extractText(resumeFile), MAX_RESUME_CHARS);
        } catch (IOException ex) {
            throw new BadRequestException("Could not parse resume file: " + ex.getMessage());
        }

        if (resumeText.isBlank()) {
            throw new BadRequestException("Could not extract readable text from resume");
        }
        processingLogs.add(stageLog("RESUME_PARSED", "Extracted characters: " + resumeText.length()));

        Optional<Applicant> applicantOptional = applicantRepository.findByEmail(userEmail);
        processingLogs.add(stageLog("PROFILE_LOOKUP", applicantOptional.isPresent() ? "Applicant profile found by email" : "No applicant profile found; scoring resume only"));

        Applicant applicant = applicantOptional.orElse(null);
        String jobText = buildJobContext(request, applicant);
        String candidateText = buildCandidateContext(request, resumeText, applicant);
        processingLogs.add(stageLog("CONTEXT_READY", "Scoring context built"));

        AiResumeScoreResult scoreResult = aiAssistantService.scoreResume(jobText, candidateText);
        double boundedScore = Math.max(0, Math.min(100, scoreResult.score()));
        String reason = scoreResult.reason() == null || scoreResult.reason().isBlank()
                ? "Talentforge AI returned a score without additional reasoning."
                : truncate(scoreResult.reason(), 900);
        String keywords = scoreResult.matchingKeywords() == null ? "" : truncate(scoreResult.matchingKeywords(), 1200);
        processingLogs.add(stageLog("AI_SCORED", "Talentforge AI score computed: " + boundedScore));

        if (applicant != null) {
            Map<String, Object> aiAnalysis = new LinkedHashMap<>();
            aiAnalysis.put("provider", "Talentforge AI");
            aiAnalysis.put("score", boundedScore);
            aiAnalysis.put("skills", keywords);
            aiAnalysis.put("reasoning", reason);
            aiAnalysis.put("processedAt", LocalDateTime.now().toString());
            aiAnalysis.put("source", "candidate_resume_score");
            aiAnalysis.put("parsedCharacters", resumeText.length());
            aiAnalysis.put("fileName", resumeFile.getOriginalFilename());

            applicant.setAiScore(boundedScore);
            applicant.setAiAnalysis(aiAnalysis);
            applicantRepository.save(applicant);
            processingLogs.add(stageLog("PROFILE_UPDATED", "Applicant ai_score and ai_analysis updated"));
        }

        return new ApplicantResumeScoreResponse(
                boundedScore,
                reason,
                keywords,
                resumeText.length(),
                "Talentforge AI",
                applicant != null,
                LocalDateTime.now(),
                processingLogs
        );
    }

    private String buildJobContext(ApplicantResumeScoreRequest request, Applicant applicant) {
        StringBuilder context = new StringBuilder();
        append(context, "Target Role", request == null ? null : request.getTargetRole());
        append(context, "Job Description", request == null ? null : request.getJobDescription());
        append(context, "Requirements", request == null ? null : request.getRequirements());

        if (context.isEmpty() && applicant != null) {
            append(context, "Target Role", "General role matching candidate profile");
            append(context, "Summary", applicant.getSummary());
            append(context, "Skills", applicant.getSkills());
        }

        if (context.isEmpty()) {
            append(context, "Target Role", "General Talentforge candidate fit assessment");
            append(context, "Requirements", "Evaluate transferable technical and communication skills.");
        }

        return truncate(context.toString(), MAX_CONTEXT_CHARS);
    }

    private String buildCandidateContext(ApplicantResumeScoreRequest request, String resumeText, Applicant applicant) {
        StringBuilder context = new StringBuilder();
        append(context, "Resume", resumeText);
        append(context, "Cover Letter", request == null ? null : request.getCoverLetter());

        if (applicant != null) {
            append(context, "Applicant Name", applicant.getFullName());
            append(context, "Summary", applicant.getSummary());
            append(context, "Skills", applicant.getSkills());
            append(context, "Experience", applicant.getYearsOfExperience() == null ? null : applicant.getYearsOfExperience() + " years");
            append(context, "Location", applicant.getLocation());
        }

        return truncate(context.toString(), MAX_CONTEXT_CHARS);
    }

    private void append(StringBuilder builder, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append(title).append(":\n").append(value.trim());
    }

    private String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() <= maxChars) {
            return cleaned;
        }
        return cleaned.substring(0, maxChars);
    }

    private String stageLog(String stage, String detail) {
        return LocalDateTime.now() + " | " + stage + " | " + detail;
    }
}
