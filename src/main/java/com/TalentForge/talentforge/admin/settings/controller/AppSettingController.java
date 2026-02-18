package com.TalentForge.talentforge.admin.settings.controller;

import com.TalentForge.talentforge.admin.settings.dto.AppSettingResponse;
import com.TalentForge.talentforge.admin.settings.dto.AppSettingsUpsertRequest;
import com.TalentForge.talentforge.admin.settings.service.AppSettingService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingService appSettingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppSettingResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<AppSettingResponse>>builder()
                .success(true)
                .message("App settings fetched")
                .data(appSettingService.getAll())
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<AppSettingResponse>>> upsert(@Valid @RequestBody AppSettingsUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<AppSettingResponse>>builder()
                .success(true)
                .message("App settings saved")
                .data(appSettingService.upsert(request.settings()))
                .build());
    }
}
