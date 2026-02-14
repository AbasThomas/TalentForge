package com.TalentForge.talentforge.note.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        Long applicationId,
        Long recruiterId,
        String recruiterName,
        String content,
        LocalDateTime createdAt
) {
}
