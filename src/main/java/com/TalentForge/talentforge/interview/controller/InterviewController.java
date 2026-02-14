package com.TalentForge.talentforge.interview.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.interview.dto.InterviewRequest;
import com.TalentForge.talentforge.interview.dto.InterviewResponse;
import com.TalentForge.talentforge.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponse>> create(@Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(ApiResponse.<InterviewResponse>builder()
                .success(true)
                .message("Interview created")
                .data(interviewService.create(request))
                .build());
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.<List<InterviewResponse>>builder()
                .success(true)
                .message("Interviews fetched")
                .data(interviewService.getByApplication(applicationId))
                .build());
    }
}
