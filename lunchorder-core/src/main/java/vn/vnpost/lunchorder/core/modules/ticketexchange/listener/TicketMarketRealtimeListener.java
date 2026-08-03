package vn.vnpost.lunchorder.core.modules.ticketexchange.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.vnpost.lunchorder.core.modules.notification.realtime.RealtimeBroadcaster;
import vn.vnpost.lunchorder.core.modules.ticketexchange.event.TicketMarketChangedEvent;

/**
 * Broadcasts market changes only after the transaction commits: a client that
 * hears the event immediately re-queries the market, and firing pre-commit would
 * hand it the stale list it already had.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketMarketRealtimeListener {

    public static final String EVENT_MARKET_CHANGED = "market-changed";

    private final RealtimeBroadcaster realtimeBroadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMarketChanged(TicketMarketChangedEvent event) {
        try {
            realtimeBroadcaster.broadcast(EVENT_MARKET_CHANGED, event);
        } catch (Exception e) {
            log.error("Failed to broadcast ticket market change {}", event.reason(), e);
        }
    }
}
