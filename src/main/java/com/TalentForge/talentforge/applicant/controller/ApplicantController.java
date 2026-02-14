package com.TalentForge.talentforge.applicant.controller;

import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.service.ApplicantService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
