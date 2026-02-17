package com.TalentForge.talentforge.notification.service;

import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.notification.dto.NotificationListResponse;
import com.TalentForge.talentforge.notification.dto.NotificationMarkAllReadResponse;
import com.TalentForge.talentforge.notification.dto.NotificationResponse;
import com.TalentForge.talentforge.notification.entity.Notification;
import com.TalentForge.talentforge.notification.entity.NotificationType;
import com.TalentForge.talentforge.notification.repository.NotificationRepository;
import com.TalentForge.talentforge.notification.websocket.NotificationWebSocketHandler;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getMyNotifications(String email, String search) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));

        List<NotificationResponse> notifications = notificationRepository.findForUser(user.getId(), search)
                .stream()
                .map(this::toResponse)
                .toList();
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(user.getId());

        return new NotificationListResponse(notifications, unreadCount);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Override
    public NotificationMarkAllReadResponse markAllRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
        int marked = notificationRepository.markAllReadForUser(user.getId(), LocalDateTime.now());
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(user.getId());
        return new NotificationMarkAllReadResponse(marked, unreadCount);
    }

    @Override
    public void createForUser(Long userId, NotificationType type, String title, String message, String link) {
        if (userId == null || type == null || title == null || title.isBlank() || message == null || message.isBlank()) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(trimToLength(title, 180))
                .message(trimToLength(message, 4000))
                .link(trimToLength(link, 255))
                .read(false)
                .build();
        Notification saved = notificationRepository.saveAndFlush(notification);

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        notificationWebSocketHandler.pushNotification(userId, toResponse(saved), unreadCount);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength);
    }
}
