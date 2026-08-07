package vn.vnpost.lunchorder.core.modules.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Freezes the daily headcount for the kitchen: once {@code AUTO_CONFIRM_TIME} on
 * the menu date has passed, every remaining {@code PENDING} order on or before
 * that date becomes {@code CONFIRMED}.
 *
 * <p>This fires earlier than {@code TICKET_LOCK_TIME} so the kitchen gets a final
 * count while the ticket market is still open. Trading a ticket after this point
 * only moves an already-counted meal to another user, so
 * {@code TicketExchangeServiceImpl} keeps the confirmed status on hand-over.</p>
 *
 * <p>Runs every minute rather than a fixed cron window, because
 * {@code AUTO_CONFIRM_TIME} is admin-configurable and a fixed window would
 * miss the trigger if an admin changes the confirm time.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoConfirmScheduler {

    private final OrderRepository orderRepository;
    private final CutOffPolicy cutOffPolicy;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void confirmPendingOrders() {
        LocalDate today = LocalDate.now();
        LocalTime confirmTime = cutOffPolicy.getAutoConfirmTime();
        LocalDate cutoffDate = LocalTime.now().isBefore(confirmTime) ? today.minusDays(1) : today;

        try {
            int confirmed = orderRepository.updateStatusByOrderDateLessThanEqualAndCurrentStatus(
                    cutoffDate, OrderStatus.PENDING, OrderStatus.CONFIRMED);
            if (confirmed > 0) {
                log.info("Auto-confirm: {} order(s) confirmed for date {} past confirm time {}", confirmed, cutoffDate, confirmTime);
            }
        } catch (Exception e) {
            log.error("Auto-confirm: failed to confirm orders for date {}", cutoffDate, e);
        }
    }
}
