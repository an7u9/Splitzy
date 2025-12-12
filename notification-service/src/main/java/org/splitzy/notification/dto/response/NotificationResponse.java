package org.splitzy.notification.dto.response;

import org.splitzy.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/** Response DTO for notification details */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long recipientUserId;
    private Long senderUserId;
    private Notification.NotificationType notificationType;
    private String title;
    private String message;
    private String description;
    private String entityType;
    private Long entityId;
    private Notification.NotificationStatus status;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Boolean emailSent;
    private Boolean websocketSent;
    private LocalDateTime createdAt;
}