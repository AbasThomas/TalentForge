package com.TalentForge.talentforge.applicant.service;

import com.TalentForge.talentforge.ai.dto.AiResumeScoreResult;
import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.ai.service.ResumeParserService;
import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreResponse;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreHistoryItemResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreTaskResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreTaskSubmitResponse;
import com.TalentForge.talentforge.applicant.entity.Applicant;
import com.TalentForge.talentforge.applicant.entity.ResumeScoreHistory;
import com.TalentForge.talentforge.applicant.entity.ResumeScoreTask;
import com.TalentForge.talentforge.applicant.entity.ResumeScoreTaskStatus;
import com.TalentForge.talentforge.applicant.mapper.ApplicantMapper;
import com.TalentForge.talentforge.applicant.repository.ApplicantRepository;
import com.TalentForge.talentforge.applicant.repository.ResumeScoreHistoryRepository;
import com.TalentForge.talentforge.applicant.repository.ResumeScoreTaskRepository;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.notification.entity.NotificationType;
import com.TalentForge.talentforge.notification.service.NotificationService;
import com.TalentForge.talentforge.subscription.service.SubscriptionLimitService;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_CONTEXT_CHARS = 14000;
    private static final AtomicInteger RESUME_TASK_THREAD_COUNTER = new AtomicInteger(0);

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;
    private final ResumeParserService resumeParserService;
    private final AiAssistantService aiAssistantService;
    private final UserRepository userRepository;
    private final SubscriptionLimitService subscriptionLimitService;
    private final NotificationService notificationService;
    private final ResumeScoreHistoryRepository resumeScoreHistoryRepository;
    private final ResumeScoreTaskRepository resumeScoreTaskRepository;
    private final ExecutorService resumeTaskExecutor = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("resume-task-" + RESUME_TASK_THREAD_COUNTER.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

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
        User user = getUserByEmailOrThrow(userEmail);
        if (user.getRole() == UserRole.CANDIDATE) {
            subscriptionLimitService.ensureCandidateCanScoreResume(user);
        }
        if (resumeFile == null || resumeFile.isEmpty()) {
            throw new BadRequestException("resumeFile is required");
        }

        List<String> processingLogs = new ArrayList<>();
        processingLogs.add(stageLog("REQUEST_RECEIVED", "Talentforge candidate resume score request received"));

        final byte[] resumeBytes;
        try {
            resumeBytes = resumeFile.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read resume file: " + ex.getMessage());
        }

        ScoreComputation result = executeScore(
                user,
                request,
                resumeFile.getOriginalFilename(),
                resumeFile.getContentType(),
                resumeFile.getSize(),
                resumeBytes,
                processingLogs,
                null
        );

        notificationService.createForUser(
                user.getId(),
                NotificationType.RESUME_PARSED_SUCCESS,
                "Resume parsing completed",
                "Resume parsing and scoring completed successfully. Latest score: " + result.score() + ".",
                buildResumeAiLink(user, null)
        );

        return new ApplicantResumeScoreResponse(
                result.score(),
                result.reason(),
                result.keywords(),
                result.parsedCharacters(),
                result.source(),
                result.usedApplicantProfile(),
                result.analyzedAt(),
                processingLogs
        );
    }

    @Override
    public List<ResumeScoreHistoryItemResponse> getResumeScoreHistory(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("Authenticated user email is required");
        }

        User user = getUserByEmailOrThrow(userEmail);

        return resumeScoreHistoryRepository.findTop100ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResumeScoreHistoryItemResponse)
                .toList();
    }

    @Override
    public ResumeScoreTaskSubmitResponse submitResumeScoreTask(String userEmail, ApplicantResumeScoreRequest request, MultipartFile resumeFile) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("Authenticated user email is required");
        }
        if (resumeFile == null || resumeFile.isEmpty()) {
            throw new BadRequestException("resumeFile is required");
        }

        User user = getUserByEmailOrThrow(userEmail);
        if (user.getRole() == UserRole.CANDIDATE) {
            subscriptionLimitService.ensureCandidateCanScoreResume(user);
            long inflightTasks = resumeScoreTaskRepository.countByUserIdAndStatusIn(
                    user.getId(),
                    List.of(ResumeScoreTaskStatus.QUEUED, ResumeScoreTaskStatus.PROCESSING)
            );
            if (inflightTasks >= 3) {
                throw new BadRequestException("You already have 3 resume parsing tasks running. Wait for completion.");
            }
        }

        final byte[] resumeBytes;
        try {
            resumeBytes = resumeFile.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read resume file: " + ex.getMessage());
        }

        ResumeScoreTask task = ResumeScoreTask.builder()
                .user(user)
                .status(ResumeScoreTaskStatus.QUEUED)
                .fileName(blankToNull(truncate(resumeFile.getOriginalFilename(), 255)))
                .fileContentType(blankToNull(truncate(resumeFile.getContentType(), 120)))
                .targetRole(blankToNull(truncate(request == null ? null : request.getTargetRole(), 140)))
                .processingLogs(stageLog("QUEUED", "Resume parsing task queued"))
                .build();
        ResumeScoreTask savedTask = resumeScoreTaskRepository.save(task);

        ApplicantResumeScoreRequest requestCopy = copyRequest(request);
        CompletableFuture.runAsync(
                () -> processResumeScoreTask(
                        savedTask.getId(),
                        user.getId(),
                        requestCopy,
                        resumeBytes,
                        resumeFile.getOriginalFilename(),
                        resumeFile.getContentType(),
                        resumeFile.getSize()
                ),
                resumeTaskExecutor
        );

        return new ResumeScoreTaskSubmitResponse(
                savedTask.getId(),
                savedTask.getStatus(),
                "Resume parsing started in background. You can leave this page.",
                savedTask.getCreatedAt()
        );
    }

    @Override
    public ResumeScoreTaskResponse getResumeScoreTask(String userEmail, Long taskId) {
        if (taskId == null) {
            throw new BadRequestException("taskId is required");
        }

        User user = getUserByEmailOrThrow(userEmail);
        ResumeScoreTask task = findTaskForUser(user, taskId);
        return toTaskResponse(task);
    }

    @Override
    public List<ResumeScoreTaskResponse> getResumeScoreTasks(String userEmail) {
        User user = getUserByEmailOrThrow(userEmail);
        return resumeScoreTaskRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toTaskResponse)
                .toList();
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

    private String buildUnreadableResumeFallback(String fileName, String contentType, long size) {
        String name = fileName == null ? "unknown-file" : fileName;
        String safeContentType = contentType == null ? "unknown" : contentType;

        return "Resume parsing note:\n"
                + "The uploaded resume appears to be image-based or has no machine-readable text.\n"
                + "File name: " + name + "\n"
                + "Content type: " + safeContentType + "\n"
                + "File size bytes: " + size + "\n"
                + "Use applicant profile, role description, and requirements to estimate fit.";
    }

    private String stageLog(String stage, String detail) {
        return LocalDateTime.now() + " | " + stage + " | " + detail;
    }

    private void processResumeScoreTask(
            Long taskId,
            Long userId,
            ApplicantResumeScoreRequest request,
            byte[] resumeBytes,
            String fileName,
            String contentType,
            long fileSize
    ) {
        ResumeScoreTask task = resumeScoreTaskRepository.findById(taskId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (task == null || user == null) {
            return;
        }

        List<String> processingLogs = parseProcessingLogs(task.getProcessingLogs());
        processingLogs.add(stageLog("PROCESSING_STARTED", "Background resume parsing started"));
        task.setStatus(ResumeScoreTaskStatus.PROCESSING);
        task.setStartedAt(LocalDateTime.now());
        task.setProcessingLogs(serializeProcessingLogs(processingLogs));
        resumeScoreTaskRepository.save(task);

        try {
            if (user.getRole() == UserRole.CANDIDATE) {
                subscriptionLimitService.ensureCandidateCanScoreResume(user);
            }

            ScoreComputation result = executeScore(user, request, fileName, contentType, fileSize, resumeBytes, processingLogs, task);
            task.setStatus(ResumeScoreTaskStatus.COMPLETED);
            task.setScore(result.score());
            task.setReason(result.reason());
            task.setMatchingKeywords(result.keywords());
            task.setParsedCharacters(result.parsedCharacters());
            task.setSource(result.source());
            task.setUsedApplicantProfile(result.usedApplicantProfile());
            task.setProcessingLogs(serializeProcessingLogs(processingLogs));
            task.setErrorMessage(null);
            task.setCompletedAt(LocalDateTime.now());
            resumeScoreTaskRepository.save(task);

            notificationService.createForUser(
                    user.getId(),
                    NotificationType.RESUME_PARSED_SUCCESS,
                    "Resume parsing completed",
                    "Background parsing completed. Latest score: " + result.score() + ".",
                    buildResumeAiLink(user, task.getId())
            );
        } catch (Exception ex) {
            processingLogs.add(stageLog("FAILED", ex.getMessage() == null ? "Resume parsing task failed" : ex.getMessage()));
            task.setStatus(ResumeScoreTaskStatus.FAILED);
            task.setErrorMessage(truncate(ex.getMessage(), 2000));
            task.setProcessingLogs(serializeProcessingLogs(processingLogs));
            task.setCompletedAt(LocalDateTime.now());
            resumeScoreTaskRepository.save(task);

            notificationService.createForUser(
                    user.getId(),
                    NotificationType.SYSTEM,
                    "Resume parsing failed",
                    "Background resume parsing failed. Open details to review logs and retry.",
                    buildResumeAiLink(user, task.getId())
            );
        }
    }

    private ScoreComputation executeScore(
            User user,
            ApplicantResumeScoreRequest request,
            String fileName,
            String contentType,
            long fileSize,
            byte[] resumeBytes,
            List<String> processingLogs,
            ResumeScoreTask linkedTask
    ) {
        String resumeText;
        try {
            resumeText = truncate(resumeParserService.extractText(fileName, contentType, resumeBytes), MAX_RESUME_CHARS);
        } catch (IOException ex) {
            throw new BadRequestException("Could not parse resume file: " + ex.getMessage());
        }

        if (resumeText.isBlank()) {
            processingLogs.add(stageLog("RESUME_EMPTY", "No readable text extracted from resume. Using metadata/context fallback."));
            resumeText = truncate(buildUnreadableResumeFallback(fileName, contentType, fileSize), MAX_RESUME_CHARS);
        }
        processingLogs.add(stageLog("RESUME_PARSED", "Extracted characters: " + resumeText.length()));

        Optional<Applicant> applicantOptional = applicantRepository.findByEmail(user.getEmail());
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
        LocalDateTime analyzedAt = LocalDateTime.now();
        processingLogs.add(stageLog("AI_SCORED", "Talentforge AI score computed: " + boundedScore));

        if (applicant != null) {
            Map<String, Object> aiAnalysis = new LinkedHashMap<>();
            aiAnalysis.put("provider", "Talentforge AI");
            aiAnalysis.put("score", boundedScore);
            aiAnalysis.put("skills", keywords);
            aiAnalysis.put("reasoning", reason);
            aiAnalysis.put("processedAt", analyzedAt.toString());
            aiAnalysis.put("source", "candidate_resume_score");
            aiAnalysis.put("parsedCharacters", resumeText.length());
            aiAnalysis.put("fileName", fileName);

            applicant.setAiScore(boundedScore);
            applicant.setAiAnalysis(aiAnalysis);
            applicantRepository.save(applicant);
            processingLogs.add(stageLog("PROFILE_UPDATED", "Applicant ai_score and ai_analysis updated"));
        }

        if (user.getRole() == UserRole.CANDIDATE) {
            subscriptionLimitService.incrementCandidateResumeScoreUsage(user);
        }

        ResumeScoreHistory history = ResumeScoreHistory.builder()
                .user(user)
                .resumeScoreTask(linkedTask)
                .score(boundedScore)
                .reason(reason)
                .matchingKeywords(keywords)
                .parsedCharacters(resumeText.length())
                .source("Talentforge AI")
                .usedApplicantProfile(applicant != null)
                .fileName(blankToNull(truncate(fileName, 255)))
                .targetRole(blankToNull(truncate(request == null ? null : request.getTargetRole(), 140)))
                .build();
        resumeScoreHistoryRepository.save(history);

        return new ScoreComputation(
                boundedScore,
                reason,
                keywords,
                resumeText.length(),
                "Talentforge AI",
                applicant != null,
                analyzedAt
        );
    }

    private String buildResumeAiLink(User user, Long taskId) {
        String basePath = "/candidate/resume-ai";
        if (taskId == null) {
            return basePath;
        }
        return basePath + "?taskId=" + taskId;
    }

    private ResumeScoreTask findTaskForUser(User user, Long taskId) {
        if (user.getRole() == UserRole.ADMIN) {
            return resumeScoreTaskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume score task not found: " + taskId));
        }

        return resumeScoreTaskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume score task not found: " + taskId));
    }

    private ResumeScoreTaskResponse toTaskResponse(ResumeScoreTask task) {
        return new ResumeScoreTaskResponse(
                task.getId(),
                task.getStatus(),
                task.getFileName(),
                task.getFileContentType(),
                task.getTargetRole(),
                task.getScore(),
                task.getReason(),
                task.getMatchingKeywords(),
                task.getParsedCharacters(),
                task.getSource(),
                task.getUsedApplicantProfile(),
                parseProcessingLogs(task.getProcessingLogs()),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt()
        );
    }

    private List<String> parseProcessingLogs(String logsText) {
        if (logsText == null || logsText.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(logsText.split("\\R")));
    }

    private String serializeProcessingLogs(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            return "";
        }
        return String.join("\n", logs);
    }

    private ApplicantResumeScoreRequest copyRequest(ApplicantResumeScoreRequest request) {
        ApplicantResumeScoreRequest copy = new ApplicantResumeScoreRequest();
        if (request == null) {
            return copy;
        }
        copy.setTargetRole(request.getTargetRole());
        copy.setJobDescription(request.getJobDescription());
        copy.setRequirements(request.getRequirements());
        copy.setCoverLetter(request.getCoverLetter());
        return copy;
    }

    private User getUserByEmailOrThrow(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + userEmail));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private ResumeScoreHistoryItemResponse toResumeScoreHistoryItemResponse(ResumeScoreHistory history) {
        ResumeScoreTask linkedTask = history.getResumeScoreTask();
        return new ResumeScoreHistoryItemResponse(
                history.getId(),
                linkedTask == null ? null : linkedTask.getId(),
                linkedTask == null ? null : linkedTask.getStatus(),
                history.getScore(),
                history.getReason(),
                history.getMatchingKeywords(),
                history.getParsedCharacters(),
                history.getSource(),
                history.getUsedApplicantProfile(),
                history.getFileName(),
                history.getTargetRole(),
                history.getCreatedAt()
        );
    }

    @PreDestroy
    public void shutdownResumeTaskExecutor() {
        resumeTaskExecutor.shutdownNow();
    }

    private record ScoreComputation(
            Double score,
            String reason,
            String keywords,
            Integer parsedCharacters,
            String source,
            Boolean usedApplicantProfile,
            LocalDateTime analyzedAt
    ) {
    }
}
