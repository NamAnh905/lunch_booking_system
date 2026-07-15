package vn.vnpost.lunchorder.core.modules.ordersummary.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderReportMailService;

import java.time.LocalDate;

/**
 * Scheduler tự động gửi báo cáo tổng hợp suất ăn hàng ngày qua email.
 * Chạy lúc 14:00 mỗi ngày. Việc dựng nội dung và gửi email được ủy quyền cho
 * {@link OrderReportMailService} để tránh trùng lặp logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportEmailScheduler {

    private final OrderReportMailService orderReportMailService;

    @Scheduled(cron = "0 0 14 * * ?")
    public void sendDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("Daily report scheduler: generating report for {}", today);

        try {
            orderReportMailService.sendDailyReportEmail(today);
            log.info("Daily report scheduler: email sent successfully for {}", today);
        } catch (Exception e) {
            log.error("Daily report scheduler: failed for {}", today, e);
        }
    }
}
