package vn.vnpost.lunchorder.core.modules.notification.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseRealtimeBroadcaster implements RealtimeBroadcaster {

    private final SseEmitterRegistry emitterRegistry;

    @Override
    public void broadcast(String eventName, Object payload) {
        emitterRegistry.broadcast(eventName, payload);
    }
}
