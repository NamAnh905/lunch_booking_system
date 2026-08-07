package vn.vnpost.lunchorder.core.modules.ordersummary.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.guestmeal.repository.GuestMealRepository;
import vn.vnpost.lunchorder.core.modules.guestmeal.repository.projection.GuestMealSummaryRow;
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
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderSummaryServiceImpl implements OrderSummaryService {

    private static final String GUEST_ROW_NAME = "Khách";

    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final OrderSummaryRepository orderSummaryRepository;
    private final GuestMealRepository guestMealRepository;
    private final OrderSummaryExcelHelper excelHelper;
    private final MealPricePolicy mealPricePolicy;

    @Override
    public DailyOrderSummaryResponse getDailySummary(LocalDate date, Long departmentId) {
        BigDecimal normalPrice = mealPricePolicy.getNormalPrice();
        List<OrderSummaryRow> rows = orderSummaryRepository.findDailySummary(date, departmentId, normalPrice);
        List<GuestMealSummaryRow> guestRows = guestMealRepository.findDailySummary(date, departmentId);

        List<OrderSummaryItemResponse> items = mergeItems(rows, guestRows);
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
        List<GuestMealSummaryRow> guestRows = guestMealRepository.findMonthlySummary(month, year, departmentId);

        List<OrderSummaryItemResponse> items = mergeItems(rows, guestRows);
        SummaryTotals totals = SummaryTotals.of(items);

        return MonthlyOrderSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalNormalMeals(totals.normalMeals())
                .totalSpecialMeals(totals.specialMeals())
                .totalAmount(totals.amount())
                .items(items)
                .dailyCounts(mergeDailyCounts(month, year, departmentId))
                .build();
    }

    private List<OrderSummaryItemResponse> mergeItems(List<OrderSummaryRow> rows,
            List<GuestMealSummaryRow> guestRows) {
        List<OrderSummaryItemResponse> items = new ArrayList<>(toItems(rows));
        items.addAll(toGuestItems(guestRows));
        items.sort(itemOrder());
        return items;
    }

    private Comparator<OrderSummaryItemResponse> itemOrder() {
        Comparator<String> ascending = Comparator.nullsLast(Collator.getInstance(VIETNAMESE)::compare);

        return Comparator.comparing(OrderSummaryItemResponse::getDepartmentName, ascending)
                .thenComparingInt(item -> item.getUserId() == null ? 1 : 0)
                .thenComparing(OrderSummaryItemResponse::getFullName, ascending);
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

    private List<OrderSummaryItemResponse> toGuestItems(List<GuestMealSummaryRow> guestRows) {
        Map<Long, List<GuestMealSummaryRow>> rowsByDepartment = guestRows.stream()
                .collect(Collectors.groupingBy(GuestMealSummaryRow::getDepartmentId, LinkedHashMap::new,
                        Collectors.toList()));

        return rowsByDepartment.values().stream().map(this::toGuestItem).toList();
    }

    private OrderSummaryItemResponse toGuestItem(List<GuestMealSummaryRow> departmentRows) {
        return OrderSummaryItemResponse.builder()
                .fullName(GUEST_ROW_NAME)
                .departmentName(departmentRows.getFirst().getDepartmentName())
                .normalMealCount(departmentRows.stream().mapToInt(row -> toInt(row.getNormalMealCount())).sum())
                .specialMealCount(departmentRows.stream().mapToInt(row -> toInt(row.getSpecialMealCount())).sum())
                .totalAmount(departmentRows.stream()
                        .map(row -> row.getTotalAmount() != null ? row.getTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .note(buildGuestNote(departmentRows))
                .build();
    }

    private String buildGuestNote(List<GuestMealSummaryRow> departmentRows) {
        String requesters = departmentRows.stream()
                .map(GuestMealSummaryRow::getRequestedByName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return requesters.isEmpty() ? null : requesters + " nhờ đặt cơm cho khách";
    }

    private List<DailyMealCountResponse> mergeDailyCounts(int month, int year, Long departmentId) {
        Map<LocalDate, Integer> countsByDate = new LinkedHashMap<>();

        orderSummaryRepository.findMonthlyDailyCounts(month, year, departmentId)
                .forEach(count -> countsByDate.merge(count.getDate(), toInt(count.getTotalMeals()), Integer::sum));

        guestMealRepository.findMonthlyDailyCounts(month, year, departmentId)
                .forEach(count -> countsByDate.merge(count.getDate(), toInt(count.getTotalMeals()), Integer::sum));

        return countsByDate.entrySet().stream()
                .map(entry -> DailyMealCountResponse.builder()
                        .date(entry.getKey())
                        .totalMeals(entry.getValue())
                        .build())
                .toList();
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
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
