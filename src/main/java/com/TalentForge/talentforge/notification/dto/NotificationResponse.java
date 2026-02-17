package com.TalentForge.talentforge.notification.dto;

import com.TalentForge.talentforge.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
