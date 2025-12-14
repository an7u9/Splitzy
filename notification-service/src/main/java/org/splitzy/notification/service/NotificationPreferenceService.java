package org.splitzy.notification.service;

import org.splitzy.notification.dto.request.NotificationPreferenceRequest;
import org.splitzy.notification.entity.NotificationPreference;
import org.splitzy.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user notification preferences
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Get user's notification preferences
     */
    @Transactional(readOnly = true)
    public NotificationPreference getUserPreferences(Long userId) {
        log.debug("Fetching notification preferences for user: {}", userId);

        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Update user's notification preferences
     */
    public NotificationPreference updatePreferences(Long userId, NotificationPreferenceRequest request) {
        log.info("Updating notification preferences for user: {}", userId);

        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().userId(userId).build());

        // Update fields if provided
        if (request.getEmailOnExpenseCreated() != null) {
            preference.setEmailOnExpenseCreated(request.getEmailOnExpenseCreated());
        }
        if (request.getEmailOnExpenseUpdated() != null) {
            preference.setEmailOnExpenseUpdated(request.getEmailOnExpenseUpdated());
        }
        if (request.getEmailOnSettlementCompleted() != null) {
            preference.setEmailOnSettlementCompleted(request.getEmailOnSettlementCompleted());
        }
        if (request.getEmailOnPaymentRequest() != null) {
            preference.setEmailOnPaymentRequest(request.getEmailOnPaymentRequest());
        }
        if (request.getEmailOnReminder() != null) {
            preference.setEmailOnReminder(request.getEmailOnReminder());
        }
        if (request.getWebsocketNotificationsEnabled() != null) {
            preference.setWebsocketNotificationsEnabled(request.getWebsocketNotificationsEnabled());
        }
        if (request.getPushNotificationsEnabled() != null) {
            preference.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            preference.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            preference.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preference.setQuietHoursEnd(request.getQuietHoursEnd());
        }
        if (request.getDigestFrequency() != null) {
            preference.setDigestFrequency(request.getDigestFrequency());
        }
        if (request.getNotificationLanguage() != null) {
            preference.setNotificationLanguage(request.getNotificationLanguage());
        }

        NotificationPreference savedPreference = preferenceRepository.save(preference);
        log.info("Notification preferences updated for user: {}", userId);

        return savedPreference;
    }

    /**
     * Create default preferences for new user
     */
    private NotificationPreference createDefaultPreferences(Long userId) {
        log.debug("Creating default notification preferences for user: {}", userId);

        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .emailOnExpenseCreated(true)
                .emailOnExpenseUpdated(true)
                .emailOnSettlementCompleted(true)
                .emailOnPaymentRequest(true)
                .emailOnReminder(true)
                .websocketNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .digestFrequency("IMMEDIATE")
                .notificationLanguage("en")
                .build();

        return preferenceRepository.save(preference);
    }

    /**
     * Check if email is enabled for type
     */
    @Transactional(readOnly = true)
    public boolean isEmailEnabledForType(Long userId, String notificationType) {
        log.debug("Checking if email enabled for type: {} for user: {}", notificationType, userId);

        NotificationPreference preference = getUserPreferences(userId);
        return switch (notificationType) {
            case "EXPENSE_CREATED" -> preference.getEmailOnExpenseCreated();
            case "EXPENSE_UPDATED" -> preference.getEmailOnExpenseUpdated();
            case "SETTLEMENT_COMPLETED" -> preference.getEmailOnSettlementCompleted();
            case "PAYMENT_REQUEST" -> preference.getEmailOnPaymentRequest();
            case "REMINDER" -> preference.getEmailOnReminder();
            default -> true;
        };
    }

    /**
     * Check if websocket notifications enabled
     */
    @Transactional(readOnly = true)
    public boolean isWebsocketEnabled(Long userId) {
        log.debug("Checking if websocket enabled for user: {}", userId);

        NotificationPreference preference = getUserPreferences(userId);
        return preference.getWebsocketNotificationsEnabled() && !preference.isInQuietHours();
    }
}