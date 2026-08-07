package vn.vnpost.lunchorder.core.modules.ticketexchange.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.ticketexchange.entity.TicketExchange;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;
import vn.vnpost.lunchorder.core.modules.ticketexchange.event.TicketExchangeExpiredEvent;
import vn.vnpost.lunchorder.core.modules.ticketexchange.event.TicketMarketChangeReason;
import vn.vnpost.lunchorder.core.modules.ticketexchange.event.TicketMarketChangedEvent;
import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Closes the ticket market: once {@code TICKET_LOCK_TIME} on the menu date has
 * passed, tickets still {@code OPEN} revert to their original owner, who must
 * then use the ticket themselves.
 *
 * <p>The market stays open past {@code AUTO_CONFIRM_TIME} on purpose — the
 * headcount sent to the kitchen is already fixed by then, so trading a ticket
 * afterwards only changes who eats, not how many meals are cooked. Order
 * confirmation is handled separately by {@code OrderAutoConfirmScheduler}.</p>
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
    private final CutOffPolicy cutOffPolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void revertExpiredMarketTickets() {
        LocalDate today = LocalDate.now();
        LocalTime lockTime = cutOffPolicy.getTicketLockTime();
        LocalDate cutoffDate = LocalTime.now().isBefore(lockTime) ? today.minusDays(1) : today;

        revertExpiredMarketTickets(cutoffDate, lockTime);
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
            User owner = ticketExchange.getOrder().getUser();
            eventPublisher.publishEvent(new TicketExchangeExpiredEvent(
                    owner.getId(),
                    ticketExchange.getOrder().getOrderDate()));
        }

        eventPublisher.publishEvent(new TicketMarketChangedEvent(TicketMarketChangeReason.EXPIRED));
    }
}
