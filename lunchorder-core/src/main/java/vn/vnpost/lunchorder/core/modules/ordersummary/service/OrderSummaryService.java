package vn.vnpost.lunchorder.core.modules.ordersummary.service;

import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;

import java.time.LocalDate;

public interface OrderSummaryService {

    DailyOrderSummaryResponse getDailySummary(LocalDate date, Long departmentId);

    MonthlyOrderSummaryResponse getMonthlySummary(int month, int year, Long departmentId);

    byte[] exportDailyExcel(LocalDate date, Long departmentId);

    byte[] exportMonthlyMatrixExcel(int month, int year, Long departmentId);
}
