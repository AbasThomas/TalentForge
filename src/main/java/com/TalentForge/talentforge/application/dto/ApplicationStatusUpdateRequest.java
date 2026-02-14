package com.TalentForge.talentforge.application.dto;

import com.TalentForge.talentforge.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusUpdateRequest(@NotNull ApplicationStatus status) {
}
