package vn.vnpost.lunchorder.core.modules.ordersummary.service;

import java.time.LocalDate;

/**
 * Sends order-summary reports (Excel attachments) to the configured admin
 * recipients. Extracted from {@code OrderSummaryService} so that querying,
 * Excel rendering and mail delivery are each a single responsibility.
 */
public interface OrderReportMailService {

    void sendDailyReportEmail(LocalDate date);

    void sendMonthlyReportEmail(int month, int year);
}
