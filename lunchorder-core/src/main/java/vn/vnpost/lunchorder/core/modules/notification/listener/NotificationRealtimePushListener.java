package vn.vnpost.lunchorder.core.modules.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.vnpost.lunchorder.core.modules.notification.event.NotificationCreatedEvent;
import vn.vnpost.lunchorder.core.modules.notification.realtime.NotificationPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimePushListener {

    private final NotificationPublisher notificationPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            notificationPublisher.publish(event.userId(), event.notification());
        } catch (Exception e) {
            log.error("Failed to push realtime notification to user {}", event.userId(), e);
        }
    }
}
