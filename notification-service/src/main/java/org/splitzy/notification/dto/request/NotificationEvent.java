package org.splitzy.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.splitzy.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event DTO for Kafka event consumption
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String eventId;
    private String eventType;
    private Long recipientUserId;
    private Long senderUserId;
    private Notification.NotificationType notificationType;
    private String title;
    private String message;
    private String description;
    private String entityType;
    private Long entityId;
    private Map<String, Object> metadata;
    private LocalDateTime eventTimestamp;
}