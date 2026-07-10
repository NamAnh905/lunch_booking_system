package vn.vnpost.lunchorder.core.modules.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoConfirmScheduler {

    private final OrderRepository orderRepository;
    private final SystemConfigRepository systemConfigRepository;

    private static final LocalTime DEFAULT_AUTO_CONFIRM_TIME = LocalTime.of(11, 0);

    /**
     * Chạy mỗi phút từ 10:55 đến 11:10 hàng ngày (bao phủ khung giờ auto-confirm).
     * Nếu chưa đến giờ auto-confirm thì bỏ qua, đảm bảo chỉ confirm đúng thời điểm.
     */
    @Scheduled(cron = "0 55-59 10 * * ?") // 10:55 - 10:59
    @Scheduled(cron = "0 0-10 11 * * ?") // 11:00 - 11:10
    @Transactional
    public void autoConfirmPendingOrders() {
        LocalTime autoConfirmTime = getAutoConfirmTime();
        LocalTime now = LocalTime.now();

        if (now.isBefore(autoConfirmTime)) {
            return;
        }

        LocalDate today = LocalDate.now();
        log.info("Auto-confirm: checking PENDING orders for date {} (trigger time: {})", today, now);

        try {
            int updatedCount = orderRepository.updateStatusByOrderDateAndCurrentStatus(
                    today,
                    OrderStatus.PENDING.name(),
                    OrderStatus.CONFIRMED.name());

            if (updatedCount > 0) {
                log.info("Auto-confirm: {} order(s) confirmed for date {}", updatedCount, today);
            } else {
                log.debug("Auto-confirm: no PENDING orders found for date {}", today);
            }
        } catch (Exception e) {
            log.error("Auto-confirm: failed to confirm orders for date {}", today, e);
        }
    }

    private LocalTime getAutoConfirmTime() {
        return systemConfigRepository.findByConfigKey("AUTO_CONFIRM_TIME")
                .map(config -> {
                    try {
                        return LocalTime.parse(config.getConfigValue());
                    } catch (Exception e) {
                        log.error("Failed to parse AUTO_CONFIRM_TIME configuration: {}", config.getConfigValue(), e);
                        return DEFAULT_AUTO_CONFIRM_TIME;
                    }
                })
                .orElse(DEFAULT_AUTO_CONFIRM_TIME);
    }
}
