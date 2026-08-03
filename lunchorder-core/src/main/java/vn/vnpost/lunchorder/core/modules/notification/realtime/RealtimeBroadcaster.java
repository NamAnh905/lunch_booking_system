package vn.vnpost.lunchorder.core.modules.notification.realtime;

/**
 * Pushes an event to every connected client rather than to a single user.
 * Used for shared state changes (e.g. the ticket market) where every viewer
 * needs to know something moved, not just the two parties involved.
 */
public interface RealtimeBroadcaster {

    void broadcast(String eventName, Object payload);
}
