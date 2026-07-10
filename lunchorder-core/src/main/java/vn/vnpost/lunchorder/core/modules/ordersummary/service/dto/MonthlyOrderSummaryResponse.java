package vn.vnpost.lunchorder.core.modules.ordersummary.service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyOrderSummaryResponse {
    private Integer month;
    private Integer year;
    private Integer totalNormalMeals;
    private Integer totalSpecialMeals;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
    private List<OrderSummaryItemResponse> items;
    private List<DailyMealCountResponse> dailyCounts;
}
