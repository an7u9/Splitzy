package org.splitzy.notification.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating notification preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {

    private Boolean emailOnExpenseCreated;
    private Boolean emailOnExpenseUpdated;
    private Boolean emailOnSettlementCompleted;
    private Boolean emailOnPaymentRequest;
    private Boolean emailOnReminder;
    private Boolean websocketNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private Boolean smsNotificationsEnabled;

    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Use HH:mm")
    private String quietHoursStart;

    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Use HH:mm")
    private String quietHoursEnd;

    @Pattern(regexp = "IMMEDIATE|DAILY|WEEKLY", message = "Digest frequency must be IMMEDIATE, DAILY, or WEEKLY")
    private String digestFrequency;

    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Invalid language code")
    private String notificationLanguage;
}
