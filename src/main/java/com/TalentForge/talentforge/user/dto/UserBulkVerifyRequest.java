package com.TalentForge.talentforge.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserBulkVerifyRequest(
        @NotEmpty List<Long> userIds,
        @NotNull Boolean verified
) {
}
