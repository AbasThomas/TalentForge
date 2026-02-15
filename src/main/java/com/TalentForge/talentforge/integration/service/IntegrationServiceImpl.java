package com.TalentForge.talentforge.integration.service;

import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.integration.dto.IntegrationBulkPublishRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationConnectionRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationConnectionResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishLogResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishResponse;
import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;
import com.TalentForge.talentforge.integration.entity.IntegrationPublishLog;
import com.TalentForge.talentforge.integration.entity.PublishStatus;
import com.TalentForge.talentforge.integration.entity.RecruiterIntegration;
import com.TalentForge.talentforge.integration.repository.IntegrationPublishLogRepository;
import com.TalentForge.talentforge.integration.repository.RecruiterIntegrationRepository;
import com.TalentForge.talentforge.job.entity.Job;
import com.TalentForge.talentforge.job.repository.JobRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.entity.UserRole;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class IntegrationServiceImpl implements IntegrationService {

    private final RecruiterIntegrationRepository recruiterIntegrationRepository;
    private final IntegrationPublishLogRepository integrationPublishLogRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    @Value("${app.frontend.public-base-url:http://localhost:3000}")
    private String frontendPublicBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationConnectionResponse> getConnections(Long recruiterId) {
        assertCurrentUserCanAccess(recruiterId);
        findRecruiter(recruiterId);

        Map<IntegrationPlatform, RecruiterIntegration> existing = recruiterIntegrationRepository
                .findByRecruiterIdOrderByPlatformAsc(recruiterId)
                .stream()
                .collect(Collectors.toMap(RecruiterIntegration::getPlatform, Function.identity()));

        return Arrays.stream(IntegrationPlatform.values())
                .sorted(Comparator.comparing(Enum::name))
                .map(platform -> {
                    RecruiterIntegration connection = existing.get(platform);
                    if (connection == null) {
                        return new IntegrationConnectionResponse(
                                null,
                                recruiterId,
                                platform,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null
                        );
                    }

                    return toConnectionResponse(connection);
                })
                .toList();
    }

    @Override
    public IntegrationConnectionResponse upsertConnection(IntegrationPlatform platform, IntegrationConnectionRequest request) {
        assertCurrentUserCanAccess(request.recruiterId());
        User recruiter = findRecruiter(request.recruiterId());

        if (isBlank(request.accountHandle()) && isBlank(request.profileUrl())) {
            throw new BadRequestException("Provide at least an account handle or profile URL");
        }

        RecruiterIntegration connection = recruiterIntegrationRepository
                .findByRecruiterIdAndPlatform(recruiter.getId(), platform)
                .orElse(RecruiterIntegration.builder()
                        .recruiter(recruiter)
                        .platform(platform)
                        .build());

        boolean wasConnected = connection.isConnected();

        connection.setAccountHandle(normalize(request.accountHandle()));
        connection.setProfileUrl(normalize(request.profileUrl()));
        connection.setDefaultMessage(normalize(request.defaultMessage()));
        connection.setConnected(request.connected() == null || request.connected());
        if (!wasConnected && connection.isConnected()) {
            connection.setConnectedAt(LocalDateTime.now());
        }

        return toConnectionResponse(recruiterIntegrationRepository.save(connection));
    }

    @Override
    public void disconnect(Long recruiterId, IntegrationPlatform platform) {
        assertCurrentUserCanAccess(recruiterId);

        RecruiterIntegration connection = recruiterIntegrationRepository
                .findByRecruiterIdAndPlatform(recruiterId, platform)
                .orElseThrow(() -> new ResourceNotFoundException("No saved connection for " + platform));

        connection.setConnected(false);
        recruiterIntegrationRepository.save(connection);
    }

    @Override
    public IntegrationPublishResponse publish(IntegrationPublishRequest request) {
        User currentUser = assertCurrentUserCanAccess(request.recruiterId());
        User recruiter = findRecruiter(request.recruiterId());
        Job job = findJobForRecruiter(request.jobId(), recruiter.getId(), currentUser);

        RecruiterIntegration connection = recruiterIntegrationRepository
                .findByRecruiterIdAndPlatform(recruiter.getId(), request.platform())
                .filter(RecruiterIntegration::isConnected)
                .orElseThrow(() -> new BadRequestException("Platform is not connected: " + request.platform()));

        IntegrationPublishLog log = createPublishLog(recruiter, job, request.platform(), request.customMessage(), connection);
        return toPublishResponse(log);
    }

    @Override
    public List<IntegrationPublishResponse> publishAll(IntegrationBulkPublishRequest request) {
        User currentUser = assertCurrentUserCanAccess(request.recruiterId());
        User recruiter = findRecruiter(request.recruiterId());
        Job job = findJobForRecruiter(request.jobId(), recruiter.getId(), currentUser);

        List<RecruiterIntegration> connections = recruiterIntegrationRepository
                .findByRecruiterIdAndConnectedTrueOrderByPlatformAsc(recruiter.getId());
        if (connections.isEmpty()) {
            throw new BadRequestException("No connected platforms found. Connect at least one platform first.");
        }

        return connections.stream()
                .map(connection -> createPublishLog(
                        recruiter,
                        job,
                        connection.getPlatform(),
                        request.customMessage(),
                        connection
                ))
                .map(this::toPublishResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationPublishLogResponse> getPublishLogs(Long recruiterId, Long jobId) {
        User currentUser = assertCurrentUserCanAccess(recruiterId);
        findRecruiter(recruiterId);

        if (jobId != null) {
            findJobForRecruiter(jobId, recruiterId, currentUser);
        }

        List<IntegrationPublishLog> logs = jobId == null
                ? integrationPublishLogRepository.findTop50ByRecruiterIdOrderByCreatedAtDesc(recruiterId)
                : integrationPublishLogRepository.findTop50ByRecruiterIdAndJobIdOrderByCreatedAtDesc(recruiterId, jobId);

        return logs.stream().map(this::toPublishLogResponse).toList();
    }

    private IntegrationPublishLog createPublishLog(
            User recruiter,
            Job job,
            IntegrationPlatform platform,
            String customMessage,
            RecruiterIntegration connection
    ) {
        String jobUrl = buildJobUrl(job.getId());
        String shareText = buildShareText(job, jobUrl, customMessage, connection.getDefaultMessage());
        String targetUrl = buildTargetUrl(platform, jobUrl, shareText, connection.getProfileUrl());

        IntegrationPublishLog log = IntegrationPublishLog.builder()
                .recruiter(recruiter)
                .job(job)
                .platform(platform)
                .targetUrl(targetUrl)
                .shareText(shareText)
                .status(PublishStatus.SUCCESS)
                .message("Ready to publish")
                .build();
        return integrationPublishLogRepository.save(log);
    }

    private Job findJobForRecruiter(Long jobId, Long recruiterId, User currentUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isAdmin && (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiterId))) {
            throw new AccessDeniedException("You can only publish your own jobs");
        }

        return job;
    }

    private User findRecruiter(Long recruiterId) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));

        if (recruiter.getRole() != UserRole.RECRUITER && recruiter.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Integrations are only available for recruiter accounts");
        }

        return recruiter;
    }

    private User assertCurrentUserCanAccess(Long recruiterId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isAdmin && !currentUser.getId().equals(recruiterId)) {
            throw new AccessDeniedException("You can only access your own integrations");
        }

        return currentUser;
    }

    private String buildJobUrl(Long jobId) {
        String normalizedBase = frontendPublicBaseUrl == null
                ? "http://localhost:3000"
                : frontendPublicBaseUrl.replaceAll("/+$", "");
        return normalizedBase + "/jobs/" + jobId;
    }

    private String buildShareText(Job job, String jobUrl, String customMessage, String defaultMessage) {
        String headline = !isBlank(customMessage) ? customMessage : defaultMessage;
        return Stream.of(
                        headline,
                        "Talentforge is hiring: " + job.getTitle(),
                        job.getLocation() == null ? null : "Location: " + job.getLocation(),
                        job.getDepartment() == null ? null : "Team: " + job.getDepartment(),
                        job.getJobType() == null ? null : "Type: " + job.getJobType().name(),
                        job.getExperienceLevel() == null ? null : "Experience: " + job.getExperienceLevel().name(),
                        "Apply: " + jobUrl,
                        "#Hiring #Jobs #Talentforge"
                )
                .filter(value -> !isBlank(value))
                .collect(Collectors.joining("\n"));
    }

    private String buildTargetUrl(IntegrationPlatform platform, String jobUrl, String shareText, String profileUrl) {
        return switch (platform) {
            case LINKEDIN -> "https://www.linkedin.com/sharing/share-offsite/?url=" + encode(jobUrl);
            case X -> "https://twitter.com/intent/tweet?text=" + encode(shareText);
            case UPWORK -> !isBlank(profileUrl) ? profileUrl.trim() : "https://www.upwork.com/";
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private IntegrationConnectionResponse toConnectionResponse(RecruiterIntegration connection) {
        return new IntegrationConnectionResponse(
                connection.getId(),
                connection.getRecruiter() == null ? null : connection.getRecruiter().getId(),
                connection.getPlatform(),
                connection.isConnected(),
                connection.getAccountHandle(),
                connection.getProfileUrl(),
                connection.getDefaultMessage(),
                connection.getConnectedAt(),
                connection.getUpdatedAt()
        );
    }

    private IntegrationPublishResponse toPublishResponse(IntegrationPublishLog log) {
        return new IntegrationPublishResponse(
                log.getId(),
                log.getRecruiter() == null ? null : log.getRecruiter().getId(),
                log.getJob() == null ? null : log.getJob().getId(),
                log.getPlatform(),
                log.getTargetUrl(),
                log.getShareText(),
                log.getStatus(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }

    private IntegrationPublishLogResponse toPublishLogResponse(IntegrationPublishLog log) {
        return new IntegrationPublishLogResponse(
                log.getId(),
                log.getRecruiter() == null ? null : log.getRecruiter().getId(),
                log.getJob() == null ? null : log.getJob().getId(),
                log.getPlatform(),
                log.getTargetUrl(),
                log.getShareText(),
                log.getStatus(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }
}
