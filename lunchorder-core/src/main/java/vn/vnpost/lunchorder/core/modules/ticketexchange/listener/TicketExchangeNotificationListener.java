package vn.vnpost.lunchorder.core.modules.ticketexchange.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.event.TicketExchangeClaimedEvent;
import vn.vnpost.lunchorder.core.modules.ticketexchange.event.TicketExchangeExpiredEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketExchangeNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTicketClaimed(TicketExchangeClaimedEvent event) {
        try {
            notificationService.sendNotificationToUser(
                    event.sellerId(),
                    "Vé ăn trưa đã được chuyển nhượng",
                    "Vé ăn trưa ngày " + event.orderDate() + " của bạn đã được " + event.buyerName()
                            + " nhận thành công.");
            notificationService.sendNotificationToUser(
                    event.buyerId(),
                    "Nhận vé ăn trưa thành công",
                    "Bạn đã nhận thành công vé ăn trưa ngày " + event.orderDate() + " từ "
                            + event.sellerName() + ".");
        } catch (Exception e) {
            log.error("Failed to send notification for ticket exchange claim, seller {} buyer {}",
                    event.sellerId(), event.buyerId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTicketExpired(TicketExchangeExpiredEvent event) {
        try {
            notificationService.sendNotificationToUser(
                    event.ownerId(),
                    "Vé ăn trưa đã hết hạn pass",
                    "Vé ăn trưa ngày " + event.orderDate()
                            + " không có ai nhận trước giờ khóa nên đã tự động thu hồi khỏi chợ. Bạn cần sử dụng vé này.");
        } catch (Exception e) {
            log.error("Failed to notify owner {} for expired ticket exchange", event.ownerId(), e);
        }
    }
}
