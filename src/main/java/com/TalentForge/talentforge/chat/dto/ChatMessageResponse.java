package com.TalentForge.talentforge.chat.dto;

import com.TalentForge.talentforge.chat.entity.SenderType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long userId,
        SenderType senderType,
        String message,
        LocalDateTime sentAt
) {
}
