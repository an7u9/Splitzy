package org.splitzy.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.common.dto.PageResponse;
import org.splitzy.common.exception.ResourceNotFoundException;
import org.splitzy.notification.dto.response.NotificationResponse;
import org.splitzy.notification.entity.Notification;
import org.splitzy.notification.repository.NotificationRepository;
import org.splitzy.notification.util.MapDoToResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MapDoToResponse mapDoToResponse;

    /** Get Unread Notifications for user. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUnreadNotification(Long userId, int page, int size){
        log.debug("Fetching unread notifications for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> unreadNotifications = notificationRepository.findByRecipientUserIdAndIsReadFalseAndIsActiveTrue(userId, pageable);

        return PageResponse.of(unreadNotifications.map(mapDoToResponse::toResponse));
    }

    /** Get all notification for user */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllNotification(Long userId, int page, int size){
        log.debug("Fetching all notifications for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository.findByRecipientUserIdAndIsActiveTrue(userId, pageable);

        return PageResponse.of(notifications.map(mapDoToResponse::toResponse));
    }

    /** Get notifications by Type */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationByType(Long userId, Notification.NotificationType type, int page, int size){
        log.debug("Fetching {} notifications for user: {}", type, userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository.findByRecipientUserIdAndNotificationTypeAndIsActiveTrue(userId, type, pageable);

        return PageResponse.of(notifications.map(mapDoToResponse::toResponse));
    }

    /** Get Single Notifications */
    public NotificationResponse getNotificationById(Long notificationId){
        log.debug("Fetching notification with ID: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        return mapDoToResponse.toResponse(notification);
    }

    /** Mark notification as read */
    public void markAsRead(Long notificationId, Long userId) {
        log.info("Marking notification {} as read for user: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to access this notification");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
        log.info("Notification marked as read");
    }

    /** Mark all notifications as read */
    public void markAllAsRead(Long userId){
        log.info("marking all notifications as read for user: {}", userId);
        notificationRepository.markAllAsRead(userId);
        log.info("All notifications marked as read for use: {}", userId);
    }

    /** Count unread notifications */
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId){
        log.debug("Counting unread notifications for user: {}", userId);
        return notificationRepository.countUnreadNotifications(userId);
    }

    /** Get recent notifications */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getRecentNotifications(Long userId, int limit){
        log.debug("Fetching {} recent notifications for user: {}", limit, userId);

        return PageResponse.<NotificationResponse>builder()
                .content(notificationRepository.findRecentNotifications(userId, limit).stream()
                        .map(mapDoToResponse::toResponse)
                        .collect(Collectors.toList()))
                .pageNumber(0)
                .pageSize(limit)
                .totalElements(notificationRepository.countUnreadNotifications(userId))
                .totalPages(1)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    /** Delete notification (soft delete) */
    public void deleteNotification(Long notificationId, Long userId) {
        log.info("Deleting notification: {} for user: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to delete this notification");
        }

        notification.setIsActive(false);
        notificationRepository.save(notification);
        log.info("Notification deleted");
    }

    /** Get notifications for specific entity */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationsForEntity(
            Long userId, String entityType, Long entityId, int page, int size) {
        log.debug("Fetching notifications for entity: {} with ID: {}", entityType, entityId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationRepository
                .findNotificationsForEntity(userId, entityType, entityId, pageable);

        return PageResponse.of(notifications.map(mapDoToResponse::toResponse));
    }

    /** Create notification (internal use) */
    public Notification createNotification(Notification notification) {
        log.info("Creating notification for user: {} of type: {}",
                notification.getRecipientUserId(), notification.getNotificationType());

        return notificationRepository.save(notification);
    }
}
