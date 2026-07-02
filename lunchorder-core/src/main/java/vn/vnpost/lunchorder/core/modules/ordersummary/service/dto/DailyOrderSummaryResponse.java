package vn.vnpost.lunchorder.core.modules.ordersummary.service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyOrderSummaryResponse {
    private LocalDate date;
    private Integer totalNormalMeals;
    private Integer totalSpecialMeals;
    private BigDecimal totalAmount;
    private List<OrderSummaryItemResponse> items;
}
