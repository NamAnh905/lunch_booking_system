package vn.vnpost.lunchorder.core.modules.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationSendRequest;

public interface NotificationService {
    Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable);
    void markAsRead(Long userId, Long notificationId);
    void markAllAsRead(Long userId);
    void sendNotification(NotificationSendRequest request);
    NotificationResponse sendNotificationToUser(Long userId, String title, String content);
    long countUnread(Long userId);
}
