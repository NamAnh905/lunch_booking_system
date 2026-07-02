package vn.vnpost.lunchorder.core.modules.ordersummary.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.email.EmailService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler tự động gửi báo cáo tổng hợp suất ăn hàng ngày qua email.
 * Chạy lúc 14:00 mỗi ngày.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportEmailScheduler {

    private final OrderSummaryService orderSummaryService;
    private final EmailService emailService;
    private final SystemConfigRepository systemConfigRepository;

    @Scheduled(cron = "0 0 14 * * ?")
    public void sendDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("Daily report scheduler: generating report for {}", today);

        try {
            // Lấy danh sách email admin từ system_config
            String[] adminEmails = getAdminEmails();
            if (adminEmails.length == 0) {
                log.warn("Daily report scheduler: no admin emails configured (ADMIN_REPORT_EMAILS)");
                return;
            }

            // Xuất Excel
            byte[] excelData = orderSummaryService.exportDailyExcel(today, null);

            // Gửi email
            String dateStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String filename = "tong_hop_suat_an_" + today.format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
            String subject = "Tổng hợp suất ăn ngày " + dateStr;
            String body = String.format("""
                <h3>Báo cáo tổng hợp suất ăn ngày %s</h3>
                <p>Vui lòng xem file đính kèm để biết chi tiết.</p>
                <p><i>Email được gửi tự động từ hệ thống LunchOrder.</i></p>
                """, dateStr);

            emailService.sendEmailWithAttachment(adminEmails, subject, body, excelData, filename);
            log.info("Daily report scheduler: email sent successfully for {}", today);

        } catch (Exception e) {
            log.error("Daily report scheduler: failed for {}", today, e);
        }
    }

    private String[] getAdminEmails() {
        return systemConfigRepository.findByConfigKey("ADMIN_REPORT_EMAILS")
                .map(config -> config.getConfigValue().split(","))
                .orElse(new String[0]);
    }
}
