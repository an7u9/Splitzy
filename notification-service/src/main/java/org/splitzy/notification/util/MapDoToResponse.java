package org.splitzy.notification.util;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.splitzy.notification.dto.response.NotificationResponse;
import org.splitzy.notification.entity.Notification;

@Mapper(componentModel = "spring",
unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MapDoToResponse {
    NotificationResponse toResponse(Notification notification);
}
