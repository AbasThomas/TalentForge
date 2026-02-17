package com.TalentForge.talentforge.notification.websocket;

import com.TalentForge.talentforge.notification.dto.NotificationPushMessage;
import com.TalentForge.talentforge.notification.dto.NotificationResponse;
import com.TalentForge.talentforge.security.JwtService;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final String EVENT_NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    private static final String TOKEN_PARAM = "token";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized websocket session"));
            return;
        }

        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToUser.put(session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cleanupSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void pushNotification(Long userId, NotificationResponse notification, long unreadCount) {
        if (userId == null || notification == null) {
            return;
        }

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        NotificationPushMessage payload = new NotificationPushMessage(
                EVENT_NOTIFICATION_CREATED,
                notification,
                unreadCount
        );

        final String messageText;
        try {
            messageText = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize notification payload userId={} notificationId={}", userId, notification.id(), ex);
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(messageText));
            } catch (Exception ex) {
                log.debug("Failed to push websocket notification to session={} userId={}", session.getId(), userId, ex);
            }
        }
    }

    private void cleanupSession(WebSocketSession session) {
        Long userId = sessionToUser.remove(session.getId());
        if (userId == null) {
            return;
        }

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        String token = extractToken(session.getUri());
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String email = jwtService.extractUsername(token);
            if (email == null || email.isBlank()) {
                return null;
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null || !user.isActive()) {
                return null;
            }
            return user.getId();
        } catch (Exception ex) {
            log.debug("Websocket token validation failed at {}", LocalDateTime.now(), ex);
            return null;
        }
    }

    private String extractToken(URI uri) {
        if (uri == null || uri.getQuery() == null || uri.getQuery().isBlank()) {
            return null;
        }

        String[] parts = uri.getQuery().split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            if (TOKEN_PARAM.equals(key)) {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
