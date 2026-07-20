package vn.vnpost.lunchorder.core.modules.notification.realtime;

import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;

public interface NotificationPublisher {

    void publish(Long userId, NotificationResponse notification);
}
