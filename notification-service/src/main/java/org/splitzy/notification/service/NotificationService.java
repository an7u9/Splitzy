package org.splitzy.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.common.dto.PageResponse;
import org.splitzy.notification.dto.response.NotificationResponse;
import org.splitzy.notification.entity.Notification;
import org.splitzy.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRespository;

    public PageResponse<NotificationResponse> getUnreadNotification(Long userId, int page, int size){
        log.debug("Fetching unread notifications for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> unreadNotifications = notificationRespository.findByRecipientUserIdAndIsReadFalseAndIsActiveTrue(userId, pageable);

        return PageResponse.of(un)
    }

}
