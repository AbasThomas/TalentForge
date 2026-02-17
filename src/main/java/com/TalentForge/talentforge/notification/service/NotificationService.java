package com.TalentForge.talentforge.notification.service;

import com.TalentForge.talentforge.notification.dto.NotificationListResponse;
import com.TalentForge.talentforge.notification.dto.NotificationMarkAllReadResponse;
import com.TalentForge.talentforge.notification.entity.NotificationType;

public interface NotificationService {

    NotificationListResponse getMyNotifications(String email, String search);

    long getUnreadCount(String email);

    NotificationMarkAllReadResponse markAllRead(String email);

    void createForUser(Long userId, NotificationType type, String title, String message, String link);
}
