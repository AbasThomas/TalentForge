package com.TalentForge.talentforge.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoteRequest(
        @NotNull Long applicationId,
        @NotNull Long recruiterId,
        @NotBlank String content
) {
}
