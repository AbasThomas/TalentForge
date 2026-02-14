package com.TalentForge.talentforge.job.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.job.dto.JobRequest;
import com.TalentForge.talentforge.job.dto.JobResponse;
import com.TalentForge.talentforge.job.service.JobService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> create(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(ApiResponse.<JobResponse>builder()
                .success(true)
                .message("Job created")
                .data(jobService.create(request))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> update(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(ApiResponse.<JobResponse>builder()
                .success(true)
                .message("Job updated")
                .data(jobService.update(id, request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> getAll(@RequestParam(required = false) Long recruiterId) {
        List<JobResponse> data = recruiterId == null ? jobService.getAll() : jobService.getByRecruiter(recruiterId);
        return ResponseEntity.ok(ApiResponse.<List<JobResponse>>builder()
                .success(true)
                .message("Jobs fetched")
                .data(data)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<JobResponse>builder()
                .success(true)
                .message("Job fetched")
                .data(jobService.getById(id))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Job deleted")
                .data(null)
                .build());
    }
}
