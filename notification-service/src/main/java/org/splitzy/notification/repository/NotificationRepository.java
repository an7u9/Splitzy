package org.splitzy.notification.repository;

import org.splitzy.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Notification entity operations
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find unread notifications for a user
     */
    Page<Notification> findByRecipientUserIdAndIsReadFalseAndIsActiveTrue(Long userId, Pageable pageable);

    /**
     * Find all notifications for a user
     */
    Page<Notification> findByRecipientUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    /**
     * Find notifications by type
     */
    Page<Notification> findByRecipientUserIdAndNotificationTypeAndIsActiveTrue(
            Long userId, Notification.NotificationType type, Pageable pageable);

    /**
     * Find pending notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.isActive = true ORDER BY n.createdAt ASC")
    Page<Notification> findPendingNotifications(Pageable pageable);

    /**
     * Find notifications that failed email delivery
     */
    @Query("SELECT n FROM Notification n WHERE n.status IN ('PENDING', 'RETRY') AND n.emailSent = false AND n.retryCount < 3 AND n.isActive = trueORDER BY n.createdAt ASC")
    List<Notification> findFailedEmailNotifications();

    /**
     * Find notifications created between dates
     */
    Page<Notification> findByRecipientUserIdAndCreatedAtBetweenAndIsActiveTrue(
            Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Count unread notifications for user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientUserId = :userId AND n.isRead = false AND n.isActive = true")
    long countUnreadNotifications(@Param("userId") Long userId);

    /**
     * Mark notifications as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipientUserId = :userId AND n.isRead = false AND n.isActive = true")
    void markAllAsRead(@Param("userId") Long userId);

    /**
     * Mark single notification as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :notificationId AND n.isActive = true")
    void markAsRead(@Param("notificationId") Long notificationId);

    /**
     * Find notifications for specific entity
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUserId = :userId AND n.entityType = :entityType AND n.entityId = :entityId AND n.isActive = trueORDER BY n.createdAt DESC")
    Page<Notification> findNotificationsForEntity(
            @Param("userId") Long userId,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable);

    /**
     * Delete old notifications (older than specified date)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isActive = false WHERE n.createdAt < :date AND n.isActive = true")
    void archiveOldNotifications(@Param("date") LocalDateTime date);

    /**
     * Find recent notifications for user
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUserId = :userId AND n.isActive = true ORDER BY n.createdAt DESC LIMIT :limit")
    List<Notification> findRecentNotifications(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Find notifications by status
     */
    Page<Notification> findByRecipientUserIdAndStatusAndIsActiveTrue(
            Long userId, Notification.NotificationStatus status, Pageable pageable);
}