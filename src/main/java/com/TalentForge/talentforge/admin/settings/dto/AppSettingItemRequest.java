package com.TalentForge.talentforge.admin.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppSettingItemRequest(
        @NotBlank(message = "key is required")
        @Size(max = 120, message = "key max length is 120")
        String key,
        @NotBlank(message = "value is required")
        @Size(max = 2000, message = "value max length is 2000")
        String value,
        @Size(max = 500, message = "description max length is 500")
        String description
) {
}
