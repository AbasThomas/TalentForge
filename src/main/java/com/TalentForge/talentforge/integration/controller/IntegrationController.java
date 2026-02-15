package com.TalentForge.talentforge.integration.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationBulkPublishRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationConnectionRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationConnectionResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishLogResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishResponse;
import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;
import com.TalentForge.talentforge.integration.service.IntegrationService;
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
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<List<IntegrationConnectionResponse>>> getConnections(@RequestParam Long recruiterId) {
        return ResponseEntity.ok(ApiResponse.<List<IntegrationConnectionResponse>>builder()
                .success(true)
                .message("Integration connections fetched")
                .data(integrationService.getConnections(recruiterId))
                .build());
    }

    @PutMapping("/connections/{platform}")
    public ResponseEntity<ApiResponse<IntegrationConnectionResponse>> upsertConnection(
            @PathVariable IntegrationPlatform platform,
            @Valid @RequestBody IntegrationConnectionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<IntegrationConnectionResponse>builder()
                .success(true)
                .message("Integration connection saved")
                .data(integrationService.upsertConnection(platform, request))
                .build());
    }

    @DeleteMapping("/connections/{platform}")
    public ResponseEntity<ApiResponse<Object>> disconnect(
            @PathVariable IntegrationPlatform platform,
            @RequestParam Long recruiterId
    ) {
        integrationService.disconnect(recruiterId, platform);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Integration disconnected")
                .data(null)
                .build());
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<IntegrationPublishResponse>> publish(@Valid @RequestBody IntegrationPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.<IntegrationPublishResponse>builder()
                .success(true)
                .message("Publish payload generated")
                .data(integrationService.publish(request))
                .build());
    }

    @PostMapping("/publish-all")
    public ResponseEntity<ApiResponse<List<IntegrationPublishResponse>>> publishAll(
            @Valid @RequestBody IntegrationBulkPublishRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.<List<IntegrationPublishResponse>>builder()
                .success(true)
                .message("Publish payloads generated")
                .data(integrationService.publishAll(request))
                .build());
    }

    @GetMapping("/publish-logs")
    public ResponseEntity<ApiResponse<List<IntegrationPublishLogResponse>>> getPublishLogs(
            @RequestParam Long recruiterId,
            @RequestParam(required = false) Long jobId
    ) {
        return ResponseEntity.ok(ApiResponse.<List<IntegrationPublishLogResponse>>builder()
                .success(true)
                .message("Publish logs fetched")
                .data(integrationService.getPublishLogs(recruiterId, jobId))
                .build());
    }
}
