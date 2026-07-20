package vn.vnpost.lunchorder.core.modules.notification.event;

import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;

public record NotificationCreatedEvent(Long userId, NotificationResponse notification) {
}
