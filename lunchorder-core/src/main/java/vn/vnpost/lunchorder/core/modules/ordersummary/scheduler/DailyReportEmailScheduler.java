package vn.vnpost.lunchorder.core.modules.ordersummary.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderReportMailService;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportEmailScheduler {

    private final OrderReportMailService orderReportMailService;
    private final CutOffPolicy cutOffPolicy;

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyReport() {
        LocalDate today = cutOffPolicy.today();

        if (cutOffPolicy.getHolidayDates().contains(today)) {
            log.info("Daily report scheduler: bỏ qua ngày lễ {}", today);
            return;
        }

        log.info("Daily report scheduler: đã giao task gửi báo cáo ngày {}", today);
        orderReportMailService.sendDailyReportEmail(today);
    }
}
