package vn.vnpost.lunchorder.core.modules.ordersummary.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.OrderSummaryRepository;
import vn.vnpost.lunchorder.core.modules.ordersummary.repository.projection.OrderSummaryRow;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.OrderSummaryService;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyMealCountResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.DailyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.MonthlyOrderSummaryResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.dto.OrderSummaryItemResponse;
import vn.vnpost.lunchorder.core.modules.ordersummary.service.helper.OrderSummaryExcelHelper;
import vn.vnpost.lunchorder.core.modules.price.service.MealPricePolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderSummaryServiceImpl implements OrderSummaryService {

    private final OrderSummaryRepository orderSummaryRepository;
    private final OrderSummaryExcelHelper excelHelper;
    private final MealPricePolicy mealPricePolicy;

    @Override
    public DailyOrderSummaryResponse getDailySummary(LocalDate date, Long departmentId) {
        BigDecimal normalPrice = mealPricePolicy.getNormalPrice();
        List<OrderSummaryRow> rows = orderSummaryRepository.findDailySummary(date, departmentId, normalPrice);

        List<OrderSummaryItemResponse> items = toItems(rows);
        SummaryTotals totals = SummaryTotals.of(items);

        return DailyOrderSummaryResponse.builder()
                .date(date)
                .totalNormalMeals(totals.normalMeals())
                .totalSpecialMeals(totals.specialMeals())
                .totalAmount(totals.amount())
                .items(items)
                .build();
    }

    @Override
    public MonthlyOrderSummaryResponse getMonthlySummary(int month, int year, Long departmentId) {
        BigDecimal normalPrice = mealPricePolicy.getNormalPrice();
        List<OrderSummaryRow> rows = orderSummaryRepository.findMonthlySummary(month, year, departmentId, normalPrice);

        List<OrderSummaryItemResponse> items = toItems(rows);
        SummaryTotals totals = SummaryTotals.of(items);

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
                .totalNormalMeals(totals.normalMeals())
                .totalSpecialMeals(totals.specialMeals())
                .totalAmount(totals.amount())
                .items(items)
                .dailyCounts(dailyCounts)
                .build();
    }

    private List<OrderSummaryItemResponse> toItems(List<OrderSummaryRow> rows) {
        return rows.stream().map(row -> OrderSummaryItemResponse.builder()
                .userId(row.getUserId())
                .fullName(row.getFullName())
                .departmentName(row.getDepartmentName())
                .normalMealCount(row.getNormalMealCount().intValue())
                .specialMealCount(row.getSpecialMealCount().intValue())
                .totalAmount(row.getTotalAmount())
                .build()).toList();
    }

    private record SummaryTotals(int normalMeals, int specialMeals, BigDecimal amount) {

        static SummaryTotals of(List<OrderSummaryItemResponse> items) {
            return new SummaryTotals(
                    items.stream().mapToInt(OrderSummaryItemResponse::getNormalMealCount).sum(),
                    items.stream().mapToInt(OrderSummaryItemResponse::getSpecialMealCount).sum(),
                    items.stream().map(OrderSummaryItemResponse::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    }

    @Override
    public byte[] exportDailyExcel(LocalDate date, Long departmentId) {
        DailyOrderSummaryResponse summary = getDailySummary(date, departmentId);
        return excelHelper.exportDailyExcel(date, summary);
    }

    @Override
    public byte[] exportMonthlyExcel(int month, int year, Long departmentId) {
        return excelHelper.exportMonthlyExcel(month, year, getMonthlySummary(month, year, departmentId));
    }
}
