package org.splitzy.notification.entity;


import jakarta.persistence.*;
import lombok.*;
import org.splitzy.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * Notification entity for storing notification history and delivery status
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient_user", columnList = "recipient_user_id"),
        @Index(name = "idx_notification_type", columnList = "notification_type"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_read_at", columnList = "read_at"),
        @Index(name = "idx_recipient_read", columnList = "recipient_user_id, read_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "sender_user_id")
    private Long senderUserId; // Optional: who triggered the notification

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "entity_type", length = 50)
    private String entityType; // EXPENSE, SETTLEMENT, SPLIT_REQUEST, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "related_data", columnDefinition = "JSON")
    private String relatedData; // JSON string for additional data

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = Boolean.FALSE;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private Boolean emailSent = Boolean.FALSE;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "websocket_sent", nullable = false)
    @Builder.Default
    private Boolean websocketSent = Boolean.FALSE;

    @Column(name = "websocket_sent_at")
    private LocalDateTime websocketSentAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = Boolean.TRUE;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Mark as email sent
     */
    public void markEmailSent() {
        this.emailSent = Boolean.TRUE;
        this.emailSentAt = LocalDateTime.now();
    }

    /**
     * Mark as websocket sent
     */
    public void markWebsocketSent() {
        this.websocketSent = Boolean.TRUE;
        this.websocketSentAt = LocalDateTime.now();
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Check if notification should be retried
     */
    public boolean shouldRetry() {
        return retryCount < 3 && status == NotificationStatus.PENDING;
    }

    /**
     * Notification types
     */
    public enum NotificationType {
        EXPENSE_CREATED,           // New expense added
        EXPENSE_UPDATED,           // Expense details updated
        SPLIT_CREATED,             // User added to an expense split
        SPLIT_SETTLED,             // Split has been settled
        SETTLEMENT_INITIATED,      // Settlement payment initiated
        SETTLEMENT_COMPLETED,      // Settlement payment completed
        SETTLEMENT_FAILED,         // Settlement payment failed
        REMINDER,                  // Payment reminder
        INVITATION,                // Group invitation
        GROUP_UPDATED,             // Group details updated
        PAYMENT_REQUEST,           // Payment request from another user
        GENERAL_ALERT,             // General system alert
        WELCOME,                   // Welcome notification
        ACTIVITY_SUMMARY           // Daily/Weekly activity summary
    }

    /**
     * Notification delivery status
     */
    public enum NotificationStatus {
        PENDING,      // Pending delivery
        DELIVERED,    // Successfully delivered
        FAILED,       // Delivery failed
        RETRY,        // Retrying delivery
        ARCHIVED      // Old notification archived
    }
}