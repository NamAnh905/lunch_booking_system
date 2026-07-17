package vn.vnpost.lunchorder.core.modules.ticketexchange.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.entity.TicketExchange;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Once {@code TICKET_LOCK_TIME} on the menu date has passed: reverts market
 * tickets still {@code OPEN} to their original owner (who must then use the
 * ticket themselves), and confirms all remaining {@code PENDING} orders for
 * that date. Both actions fire from the same config value because the order
 * list is only final once the market is closed — there is no reason to
 * confirm earlier or later than that.
 *
 * <p>Runs every minute rather than a fixed cron window, because
 * {@code TICKET_LOCK_TIME} is admin-configurable and a fixed window would
 * miss the trigger if an admin changes the lock time.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketExchangeAutoRevertScheduler {

    private final TicketExchangeRepository ticketExchangeRepository;
    private final OrderRepository orderRepository;
    private final CutOffPolicy cutOffPolicy;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void revertExpiredMarketTicketsAndConfirmOrders() {
        LocalTime lockTime = cutOffPolicy.getTicketLockTime();
        if (LocalTime.now().isBefore(lockTime)) {
            return;
        }

        LocalDate today = LocalDate.now();
        revertExpiredMarketTickets(today, lockTime);
        confirmPendingOrders(today, lockTime);
    }

    private void revertExpiredMarketTickets(LocalDate today, LocalTime lockTime) {
        List<TicketExchange> expiring = ticketExchangeRepository
                .findByStatusAndOrderDateLessThanEqual(TicketExchangeStatus.OPEN, today);
        if (expiring.isEmpty()) {
            return;
        }

        int updated = ticketExchangeRepository.updateStatusByOrderDateLessThanEqualAndCurrentStatus(
                today, TicketExchangeStatus.OPEN, TicketExchangeStatus.EXPIRED);
        log.info("Auto-revert: {} market ticket(s) expired past lock time {}", updated, lockTime);

        for (TicketExchange ticketExchange : expiring) {
            try {
                User owner = ticketExchange.getOrder().getUser();
                notificationService.sendNotificationToUser(
                        owner.getId(),
                        "Vé ăn trưa đã hết hạn pass",
                        "Vé ăn trưa ngày " + ticketExchange.getOrder().getOrderDate()
                                + " không có ai nhận trước giờ khóa nên đã tự động thu hồi khỏi chợ. Bạn cần sử dụng vé này.");
            } catch (Exception e) {
                log.error("Auto-revert: failed to notify owner for exchange {}", ticketExchange.getId(), e);
            }
        }
    }

    private void confirmPendingOrders(LocalDate today, LocalTime lockTime) {
        try {
            int confirmed = orderRepository.updateStatusByOrderDateAndCurrentStatus(
                    today, OrderStatus.PENDING, OrderStatus.CONFIRMED);
            if (confirmed > 0) {
                log.info("Auto-confirm: {} order(s) confirmed for date {} past lock time {}", confirmed, today, lockTime);
            }
        } catch (Exception e) {
            log.error("Auto-confirm: failed to confirm orders for date {}", today, e);
        }
    }
}
