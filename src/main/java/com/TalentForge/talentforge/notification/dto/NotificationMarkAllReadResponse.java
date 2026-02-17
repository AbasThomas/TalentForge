package com.TalentForge.talentforge.notification.dto;

public record NotificationMarkAllReadResponse(
        int markedCount,
        long unreadCount
) {
}
