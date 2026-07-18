package vn.vnpost.lunchorder.core.modules.ordersummary.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.systemconfig.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderReportMailService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;
import vn.vnpost.lunchorder.tools.mail.EmailService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderReportMailServiceImpl implements OrderReportMailService {

    private static final String ADMIN_REPORT_EMAILS_KEY = "ADMIN_REPORT_EMAILS";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    private final OrderSummaryService orderSummaryService;
    private final EmailService emailService;
    private final SystemConfigRepository systemConfigRepository;

    /**
     * Chạy bất đồng bộ: controller trả 202 Accepted ngay, việc kết xuất Excel và gửi mail diễn ra
     * ở luồng nền. Do lỗi không còn được trả về client đồng bộ (kể cả
     * {@link ErrorCode#ADMIN_REPORT_EMAIL_NOT_CONFIGURED}), mọi ngoại lệ được bắt và log rõ ràng
     * tại đây thay vì ném ra ngoài.
     */
    @Override
    @Async
    public void sendDailyReportEmail(LocalDate date) {
        try {
            byte[] excelData = orderSummaryService.exportDailyExcel(date, null);
            String dateStr = date.format(DISPLAY_DATE);
            String filename = "tong_hop_suat_an_" + date.format(FILE_DATE) + ".xlsx";
            String subject = "Tổng hợp suất ăn ngày " + dateStr;
            String body = String.format("""
                    <h3>Báo cáo tổng hợp suất ăn ngày %s</h3>
                    <p>Vui lòng xem file đính kèm để biết chi tiết.</p>
                    <p><i>Email được gửi tự động từ hệ thống LunchOrder.</i></p>
                    """, dateStr);

            sendToAdmins(subject, body, excelData, filename);
            log.info("Đã gửi email báo cáo tổng hợp suất ăn ngày {}", date);
        } catch (Exception e) {
            log.error("Gửi email báo cáo tổng hợp suất ăn ngày {} thất bại: {}", date, e.getMessage(), e);
        }
    }

    /**
     * Chạy bất đồng bộ. Xem ghi chú xử lý lỗi ở {@link #sendDailyReportEmail(LocalDate)}.
     */
    @Override
    @Async
    public void sendMonthlyReportEmail(int month, int year) {
        try {
            byte[] excelData = orderSummaryService.exportMonthlyMatrixExcel(month, year, null);
            String filename = "theo_doi_dat_com_thang_" + month + "_" + year + ".xlsx";
            String subject = "Báo cáo tổng hợp đặt cơm tháng " + month + "/" + year;
            String body = String.format("""
                    <h3>Báo cáo tổng hợp đặt cơm tháng %d/%d</h3>
                    <p>Vui lòng xem file đính kèm để biết chi tiết.</p>
                    <p><i>Email được gửi tự động từ hệ thống LunchOrder.</i></p>
                    """, month, year);

            sendToAdmins(subject, body, excelData, filename);
            log.info("Đã gửi email báo cáo tổng hợp đặt cơm tháng {}/{}", month, year);
        } catch (Exception e) {
            log.error("Gửi email báo cáo tổng hợp đặt cơm tháng {}/{} thất bại: {}", month, year, e.getMessage(), e);
        }
    }

    private void sendToAdmins(String subject, String body, byte[] attachment, String filename) {
        String[] adminEmails = resolveAdminEmails();
        emailService.sendEmailWithAttachment(adminEmails, subject, body, attachment, filename);
    }

    private String[] resolveAdminEmails() {
        String[] adminEmails = systemConfigRepository.findByConfigKey(ADMIN_REPORT_EMAILS_KEY)
                .map(config -> config.getConfigValue().split(","))
                .orElse(new String[0]);

        if (adminEmails.length == 0 || adminEmails[0].trim().isEmpty()) {
            throw new AppException(ErrorCode.ADMIN_REPORT_EMAIL_NOT_CONFIGURED);
        }
        return adminEmails;
    }
}
