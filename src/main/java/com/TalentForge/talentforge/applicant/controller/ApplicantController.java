package com.TalentForge.talentforge.applicant.controller;

import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreResponse;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreHistoryItemResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreTaskResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreTaskSubmitResponse;
import com.TalentForge.talentforge.applicant.service.ApplicantService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantService applicantService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicantResponse>> create(@Valid @RequestBody ApplicantRequest request) {
        return ResponseEntity.ok(ApiResponse.<ApplicantResponse>builder()
                .success(true)
                .message("Applicant created")
                .data(applicantService.create(request))
                .build());
    }

    @PostMapping(value = "/resume-score", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ApplicantResumeScoreResponse>> scoreResume(
            @ModelAttribute ApplicantResumeScoreRequest request,
            @RequestPart("resumeFile") MultipartFile resumeFile,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<ApplicantResumeScoreResponse>builder()
                .success(true)
                .message("Resume scored")
                .data(applicantService.scoreResume(authentication.getName(), request, resumeFile))
                .build());
    }

    @GetMapping("/resume-score/history")
    public ResponseEntity<ApiResponse<List<ResumeScoreHistoryItemResponse>>> getResumeScoreHistory(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<ResumeScoreHistoryItemResponse>>builder()
                .success(true)
                .message("Resume score history fetched")
                .data(applicantService.getResumeScoreHistory(authentication.getName()))
                .build());
    }

    @PostMapping(value = "/resume-score/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResumeScoreTaskSubmitResponse>> submitResumeScoreTask(
            @ModelAttribute ApplicantResumeScoreRequest request,
            @RequestPart("resumeFile") MultipartFile resumeFile,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<ResumeScoreTaskSubmitResponse>builder()
                .success(true)
                .message("Resume parsing task submitted")
                .data(applicantService.submitResumeScoreTask(authentication.getName(), request, resumeFile))
                .build());
    }

    @GetMapping("/resume-score/tasks")
    public ResponseEntity<ApiResponse<List<ResumeScoreTaskResponse>>> getResumeScoreTasks(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<List<ResumeScoreTaskResponse>>builder()
                .success(true)
                .message("Resume score tasks fetched")
                .data(applicantService.getResumeScoreTasks(authentication.getName()))
                .build());
    }

    @GetMapping("/resume-score/tasks/{taskId}")
    public ResponseEntity<ApiResponse<ResumeScoreTaskResponse>> getResumeScoreTask(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.<ResumeScoreTaskResponse>builder()
                .success(true)
                .message("Resume score task fetched")
                .data(applicantService.getResumeScoreTask(authentication.getName(), taskId))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicantResponse>> update(@PathVariable Long id, @Valid @RequestBody ApplicantRequest request) {
        return ResponseEntity.ok(ApiResponse.<ApplicantResponse>builder()
                .success(true)
                .message("Applicant updated")
                .data(applicantService.update(id, request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApplicantResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<ApplicantResponse>>builder()
                .success(true)
                .message("Applicants fetched")
                .data(applicantService.getAll())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicantResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ApplicantResponse>builder()
                .success(true)
                .message("Applicant fetched")
                .data(applicantService.getById(id))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        applicantService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder().success(true).message("Applicant deleted").data(null).build());
    }
}
