package com.TalentForge.talentforge.notification.repository;

import com.TalentForge.talentforge.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.user.id = :userId
              AND (
                  :search IS NULL OR :search = ''
                  OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findForUser(@Param("userId") Long userId, @Param("search") String search);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = :readAt
            WHERE n.user.id = :userId
              AND n.read = false
            """)
    int markAllReadForUser(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
