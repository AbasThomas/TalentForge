package com.TalentForge.talentforge.notification.dto;

public record NotificationPushMessage(
        String event,
        NotificationResponse notification,
        long unreadCount
) {
}
