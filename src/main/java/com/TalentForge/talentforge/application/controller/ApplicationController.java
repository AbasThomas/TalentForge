package com.TalentForge.talentforge.application.controller;

import com.TalentForge.talentforge.application.dto.ApplicationCreateRequest;
import com.TalentForge.talentforge.application.dto.ApplicationResponse;
import com.TalentForge.talentforge.application.dto.ApplicationStatusUpdateRequest;
import com.TalentForge.talentforge.application.service.ApplicationService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ApplicationResponse>> submit(
            @Valid @ModelAttribute ApplicationCreateRequest request,
            @RequestPart(value = "resumeFile", required = false) MultipartFile resumeFile,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<ApplicationResponse>builder()
                .success(true)
                .message("Application submitted")
                .data(applicationService.submit(request, resumeFile, authentication == null ? null : authentication.getName()))
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<ApplicationResponse>builder()
                .success(true)
                .message("Application status updated")
                .data(applicationService.updateStatus(id, request.status()))
                .build());
    }

    @PostMapping("/{id}/rescore")
    public ResponseEntity<ApiResponse<ApplicationResponse>> rescore(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ApplicationResponse>builder()
                .success(true)
                .message("Application AI score refreshed")
                .data(applicationService.rescore(id))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ApplicationResponse>builder()
                .success(true)
                .message("Application fetched")
                .data(applicationService.getById(id))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getByFilter(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Long applicantId
    ) {
        List<ApplicationResponse> data;
        if (jobId != null) {
            data = applicationService.getByJobId(jobId);
        } else if (applicantId != null) {
            data = applicationService.getByApplicantId(applicantId);
        } else {
            data = List.of();
        }

        return ResponseEntity.ok(ApiResponse.<List<ApplicationResponse>>builder()
                .success(true)
                .message("Applications fetched")
                .data(data)
                .build());
    }
}
