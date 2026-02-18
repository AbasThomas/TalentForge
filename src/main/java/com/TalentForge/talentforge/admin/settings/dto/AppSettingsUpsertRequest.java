package com.TalentForge.talentforge.admin.settings.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AppSettingsUpsertRequest(
        @NotEmpty(message = "settings is required")
        List<@Valid AppSettingItemRequest> settings
) {
}
