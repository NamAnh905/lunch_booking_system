package vn.vnpost.lunchorder.core.modules.ticketexchange.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.entity.TicketExchange;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Sweeps market tickets still {@code OPEN} once {@code TICKET_LOCK_TIME} on
 * their menu date has passed and reverts them to their original owner (who
 * must then use the ticket themselves).
 *
 * <p>Runs every minute rather than a fixed window like
 * {@code OrderAutoConfirmScheduler}, because {@code TICKET_LOCK_TIME} is
 * admin-configurable and a fixed cron window tied to the 10:30 default would
 * miss the revert if an admin changes the lock time.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketExchangeAutoRevertScheduler {

    private final TicketExchangeRepository ticketExchangeRepository;
    private final CutOffPolicy cutOffPolicy;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void revertExpiredMarketTickets() {
        LocalTime lockTime = cutOffPolicy.getTicketLockTime();
        if (LocalTime.now().isBefore(lockTime)) {
            return;
        }

        LocalDate today = LocalDate.now();
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
}
