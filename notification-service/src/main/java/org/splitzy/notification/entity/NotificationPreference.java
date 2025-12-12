package org.splitzy.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.splitzy.common.entity.BaseEntity;

/**
 * User notification preferences for controlling notification delivery
 */
@Entity
@Table(name = "notification_preferences", indexes = {
        @Index(name = "idx_user", columnList = "user_id", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "email_on_expense_created", nullable = false)
    @Builder.Default
    private Boolean emailOnExpenseCreated = Boolean.TRUE;

    @Column(name = "email_on_expense_updated", nullable = false)
    @Builder.Default
    private Boolean emailOnExpenseUpdated = Boolean.TRUE;

    @Column(name = "email_on_settlement_completed", nullable = false)
    @Builder.Default
    private Boolean emailOnSettlementCompleted = Boolean.TRUE;

    @Column(name = "email_on_payment_request", nullable = false)
    @Builder.Default
    private Boolean emailOnPaymentRequest = Boolean.TRUE;

    @Column(name = "email_on_reminder", nullable = false)
    @Builder.Default
    private Boolean emailOnReminder = Boolean.TRUE;

    @Column(name = "websocket_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean websocketNotificationsEnabled = Boolean.TRUE;

    @Column(name = "push_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean pushNotificationsEnabled = Boolean.TRUE;

    @Column(name = "sms_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean smsNotificationsEnabled = Boolean.FALSE;

    @Column(name = "quiet_hours_start", length = 5) // HH:mm format
    private String quietHoursStart;

    @Column(name = "quiet_hours_end", length = 5) // HH:mm format
    private String quietHoursEnd;

    @Column(name = "digest_frequency", length = 20) // IMMEDIATE, DAILY, WEEKLY
    @Builder.Default
    private String digestFrequency = "IMMEDIATE";

    @Column(name = "notification_language", length = 5)
    @Builder.Default
    private String notificationLanguage = "en";

    /** Check if current time is within quiet hours */
    public boolean isInQuietHours() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        java.time.LocalTime currentTime = java.time.LocalTime.now();
        java.time.LocalTime start = java.time.LocalTime.parse(quietHoursStart);
        java.time.LocalTime end = java.time.LocalTime.parse(quietHoursEnd);

        if (start.isBefore(end)) {
            return !currentTime.isBefore(start) && currentTime.isBefore(end);
        } else {
            // Quiet hours span midnight
            return !currentTime.isBefore(start) || currentTime.isBefore(end);
        }
    }

    /** Check if email notifications are enabled for type */
    public boolean isEmailEnabledForType(Notification.NotificationType type) {
        return switch (type) {
            case EXPENSE_CREATED -> emailOnExpenseCreated;
            case EXPENSE_UPDATED -> emailOnExpenseUpdated;
            case SETTLEMENT_COMPLETED -> emailOnSettlementCompleted;
            case PAYMENT_REQUEST -> emailOnPaymentRequest;
            case REMINDER -> emailOnReminder;
            default -> true;
        };
    }
}