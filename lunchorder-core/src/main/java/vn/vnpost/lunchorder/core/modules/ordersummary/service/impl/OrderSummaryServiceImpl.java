package vn.vnpost.lunchorder.core.modules.ordersummary.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.OrderSummaryRepository;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.MonthlyOrderDetail;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.OrderSummaryRow;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyMealCountResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.OrderSummaryItemResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.helper.OrderSummaryExcelHelper;
import vn.vnpost.lunchorder.core.modules.payment.repository.PaymentRepository;
import vn.vnpost.lunchorder.core.modules.price.service.MealPricePolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final OrderSummaryExcelHelper excelHelper;
    private final MealPricePolicy mealPricePolicy;

    @Override
    public DailyOrderSummaryResponse getDailySummary(LocalDate date, Long departmentId) {
        BigDecimal normalPrice = mealPricePolicy.getNormalPrice();
        List<OrderSummaryRow> rows = orderSummaryRepository.findDailySummary(date, departmentId, normalPrice);

        List<OrderSummaryItemResponse> items = rows.stream().map(row -> OrderSummaryItemResponse.builder()
                .userId(row.getUserId())
                .fullName(row.getFullName())
                .departmentName(row.getDepartmentName())
                .normalMealCount(row.getNormalMealCount().intValue())
                .specialMealCount(row.getSpecialMealCount().intValue())
                .totalAmount(row.getTotalAmount())
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
        BigDecimal normalPrice = mealPricePolicy.getNormalPrice();
        List<OrderSummaryRow> rows = orderSummaryRepository.findMonthlySummary(month, year, departmentId, normalPrice);

        // Tổng tiền đã thanh toán nhóm theo userId
        Map<Long, BigDecimal> paidMap = new HashMap<>();
        for (Object[] row : paymentRepository.sumGroupByUserForMonth(month, year)) {
            paidMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<OrderSummaryItemResponse> items = rows.stream().map(row -> {
            BigDecimal totalAmount = row.getTotalAmount();
            BigDecimal totalPaid = paidMap.getOrDefault(row.getUserId(), BigDecimal.ZERO);

            return OrderSummaryItemResponse.builder()
                    .userId(row.getUserId())
                    .fullName(row.getFullName())
                    .departmentName(row.getDepartmentName())
                    .normalMealCount(row.getNormalMealCount().intValue())
                    .specialMealCount(row.getSpecialMealCount().intValue())
                    .totalAmount(totalAmount)
                    .totalPaid(totalPaid)
                    .remainingAmount(totalAmount.subtract(totalPaid))
                    .build();
        }).toList();

        // Daily counts for the calendar view (grouped at the database level)
        List<DailyMealCountResponse> dailyCounts = orderSummaryRepository
                .findMonthlyDailyCounts(month, year, departmentId).stream()
                .map(c -> DailyMealCountResponse.builder()
                        .date(c.getDate())
                        .totalMeals(c.getTotalMeals().intValue())
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
    public byte[] exportMonthlyMatrixExcel(int month, int year, Long departmentId) {
        MonthlyOrderSummaryResponse summary = getMonthlySummary(month, year, departmentId);

        BigDecimal normalPrice = mealPricePolicy.getNormalPrice();
        BigDecimal specialPrice = mealPricePolicy.getSpecialPrice();

        List<MonthlyOrderDetail> detailRecords = orderSummaryRepository.findMonthlyOrderDetails(month, year,
                departmentId, normalPrice);

        return excelHelper.exportMonthlyMatrixExcel(month, year, summary, detailRecords, normalPrice, specialPrice);
    }
}
