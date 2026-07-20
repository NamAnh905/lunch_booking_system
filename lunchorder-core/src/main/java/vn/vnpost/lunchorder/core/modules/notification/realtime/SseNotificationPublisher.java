package vn.vnpost.lunchorder.core.modules.notification.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;

@Component
@RequiredArgsConstructor
public class SseNotificationPublisher implements NotificationPublisher {

    public static final String EVENT_NOTIFICATION = "notification";

    private final SseEmitterRegistry emitterRegistry;

    @Override
    public void publish(Long userId, NotificationResponse notification) {
        emitterRegistry.send(userId, EVENT_NOTIFICATION, notification);
    }
}
