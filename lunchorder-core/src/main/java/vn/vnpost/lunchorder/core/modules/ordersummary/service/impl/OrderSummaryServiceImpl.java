package vn.vnpost.lunchorder.core.modules.ordersummary.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.OrderSummaryRepository;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.OrderSummaryItemResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.helper.OrderSummaryExcelHelper;
import vn.vnpost.lunchorder.core.modules.payment.repository.PaymentRepository;
import vn.vnpost.lunchorder.core.modules.email.EmailService;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.price.service.PriceService;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderSummaryServiceImpl implements OrderSummaryService {

    private final OrderSummaryRepository orderSummaryRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final SystemConfigRepository systemConfigRepository;
    private final OrderSummaryExcelHelper excelHelper;
    private final PriceService priceService;

    private BigDecimal getNormalPrice() {
        return priceService.getActivePrices().stream()
                .map(PriceResponse::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(new BigDecimal("25000"));
    }

    private BigDecimal getSpecialPrice() {
        return priceService.getActivePrices().stream()
                .map(PriceResponse::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(new BigDecimal("40000"));
    }

    @Override
    public DailyOrderSummaryResponse getDailySummary(LocalDate date, Long departmentId) {
        BigDecimal normalPrice = getNormalPrice();
        List<Object[]> rawData = orderSummaryRepository.findDailySummary(date, departmentId, normalPrice);

        List<OrderSummaryItemResponse> items = rawData.stream().map(row -> OrderSummaryItemResponse.builder()
                .userId((Long) row[0])
                .fullName((String) row[1])
                .departmentName((String) row[2])
                .normalMealCount(((Number) row[3]).intValue())
                .specialMealCount(((Number) row[4]).intValue())
                .totalAmount((BigDecimal) row[5])
                .build()).toList();

        return DailyOrderSummaryResponse.builder()
                .date(date)
                .totalNormalMeals(items.stream().mapToInt(OrderSummaryItemResponse::getNormalMealCount).sum())
                .totalSpecialMeals(items.stream().mapToInt(OrderSummaryItemResponse::getSpecialMealCount).sum())
                .totalAmount(items.stream().map(OrderSummaryItemResponse::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .items(items)
                .build();
    }

    @Override
    public MonthlyOrderSummaryResponse getMonthlySummary(int month, int year, Long departmentId) {
        BigDecimal normalPrice = getNormalPrice();
        List<Object[]> rawData = orderSummaryRepository.findMonthlySummary(month, year, departmentId, normalPrice);

        // Lấy tổng tiền đã thanh toán nhóm theo userId
        Map<Long, BigDecimal> paidMap = new HashMap<>();
        List<Object[]> paidData = paymentRepository.sumGroupByUserForMonth(month, year);
        for (Object[] row : paidData) {
            paidMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<OrderSummaryItemResponse> items = rawData.stream().map(row -> {
            Long userId = (Long) row[0];
            BigDecimal totalAmount = (BigDecimal) row[5];
            BigDecimal totalPaid = paidMap.getOrDefault(userId, BigDecimal.ZERO);

            return OrderSummaryItemResponse.builder()
                    .userId(userId)
                    .fullName((String) row[1])
                    .departmentName((String) row[2])
                    .normalMealCount(((Number) row[3]).intValue())
                    .specialMealCount(((Number) row[4]).intValue())
                    .totalAmount(totalAmount)
                    .totalPaid(totalPaid)
                    .remainingAmount(totalAmount.subtract(totalPaid))
                    .build();
        }).toList();

        // Calculate daily counts for calendar view
        List<Object[]> detailRecords = orderSummaryRepository.findMonthlyOrderDetails(month, year, departmentId,
                normalPrice);
        Map<LocalDate, Integer> dailyCountMap = new HashMap<>();

        for (Object[] row : detailRecords) {
            if (row[0] == null || row[1] == null)
                continue;

            Object dateObj = row[1];
            LocalDate date = null;
            if (dateObj instanceof LocalDate) {
                date = (LocalDate) dateObj;
            } else if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof java.util.Date) {
                date = ((java.util.Date) dateObj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            } else {
                date = LocalDate.parse(dateObj.toString());
            }

            if (date != null) {
                dailyCountMap.put(date, dailyCountMap.getOrDefault(date, 0) + 1);
            }
        }

        List<vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyMealCountResponse> dailyCounts = dailyCountMap
                .entrySet().stream()
                .map(e -> vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyMealCountResponse.builder()
                        .date(e.getKey())
                        .totalMeals(e.getValue())
                        .build())
                .toList();

        return MonthlyOrderSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalNormalMeals(items.stream().mapToInt(OrderSummaryItemResponse::getNormalMealCount).sum())
                .totalSpecialMeals(items.stream().mapToInt(OrderSummaryItemResponse::getSpecialMealCount).sum())
                .totalAmount(items.stream().map(OrderSummaryItemResponse::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalPaid(items.stream().map(OrderSummaryItemResponse::getTotalPaid)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalRemaining(items.stream().map(OrderSummaryItemResponse::getRemainingAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .items(items)
                .dailyCounts(dailyCounts)
                .build();
    }

    @Override
    public byte[] exportDailyExcel(LocalDate date, Long departmentId) {
        DailyOrderSummaryResponse summary = getDailySummary(date, departmentId);
        return excelHelper.exportDailyExcel(date, summary);
    }

    @Override
    public void sendDailyReportEmail(LocalDate date) {
        String[] adminEmails = systemConfigRepository.findByConfigKey("ADMIN_REPORT_EMAILS")
                .map(config -> config.getConfigValue().split(","))
                .orElse(new String[0]);

        if (adminEmails.length == 0 || adminEmails[0].trim().isEmpty()) {
            throw new RuntimeException("Chưa cấu hình email nhận báo cáo trong system_config (ADMIN_REPORT_EMAILS)");
        }

        byte[] excelData = exportDailyExcel(date, null);
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String filename = "tong_hop_suat_an_" + date.format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        String subject = "Tổng hợp suất ăn ngày " + dateStr;
        String body = String.format("""
                <h3>Báo cáo tổng hợp suất ăn ngày %s</h3>
                <p>Vui lòng xem file đính kèm để biết chi tiết.</p>
                <p><i>Email được gửi tự động từ hệ thống LunchOrder.</i></p>
                """, dateStr);

        emailService.sendEmailWithAttachment(adminEmails, subject, body, excelData, filename);
    }

    @Override
    public byte[] exportMonthlyMatrixExcel(int month, int year, Long departmentId) {
        MonthlyOrderSummaryResponse summary = getMonthlySummary(month, year, departmentId);

        BigDecimal normalPrice = getNormalPrice();
        BigDecimal specialPrice = getSpecialPrice();

        // Fetch detail order records for matrix mapping: [userId, menuDate, isSpecial]
        List<Object[]> detailRecords = orderSummaryRepository.findMonthlyOrderDetails(month, year, departmentId,
                normalPrice);

        return excelHelper.exportMonthlyMatrixExcel(month, year, summary, detailRecords, normalPrice, specialPrice);
    }

    @Override
    public void sendMonthlyReportEmail(int month, int year) {
        String[] adminEmails = systemConfigRepository.findByConfigKey("ADMIN_REPORT_EMAILS")
                .map(config -> config.getConfigValue().split(","))
                .orElse(new String[0]);

        if (adminEmails.length == 0 || adminEmails[0].trim().isEmpty()) {
            throw new RuntimeException("Chưa cấu hình email nhận báo cáo trong system_config (ADMIN_REPORT_EMAILS)");
        }

        byte[] excelData = exportMonthlyMatrixExcel(month, year, null);
        String filename = "theo_doi_dat_com_thang_" + month + "_" + year + ".xlsx";
        String subject = "Báo cáo tổng hợp đặt cơm tháng " + month + "/" + year;
        String body = String.format("""
                <h3>Báo cáo tổng hợp đặt cơm tháng %d/%d</h3>
                <p>Vui lòng xem file đính kèm để biết chi tiết.</p>
                <p><i>Email được gửi tự động từ hệ thống LunchOrder.</i></p>
                """, month, year);

        emailService.sendEmailWithAttachment(adminEmails, subject, body, excelData, filename);
    }
}
