package com.TalentForge.talentforge.notification.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.notification.dto.NotificationListResponse;
import com.TalentForge.talentforge.notification.dto.NotificationMarkAllReadResponse;
import com.TalentForge.talentforge.notification.dto.NotificationUnreadCountResponse;
import com.TalentForge.talentforge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getMyNotifications(
            Authentication authentication,
            @RequestParam(required = false) String search
    ) {
        NotificationListResponse response = notificationService.getMyNotifications(authentication.getName(), search);
        return ResponseEntity.ok(ApiResponse.<NotificationListResponse>builder()
                .success(true)
                .message("Notifications fetched")
                .data(response)
                .build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(Authentication authentication) {
        long unreadCount = notificationService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<NotificationUnreadCountResponse>builder()
                .success(true)
                .message("Unread notification count fetched")
                .data(new NotificationUnreadCountResponse(unreadCount))
                .build());
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<NotificationMarkAllReadResponse>> markAllRead(Authentication authentication) {
        NotificationMarkAllReadResponse response = notificationService.markAllRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<NotificationMarkAllReadResponse>builder()
                .success(true)
                .message("Notifications marked as read")
                .data(response)
                .build());
    }
}
