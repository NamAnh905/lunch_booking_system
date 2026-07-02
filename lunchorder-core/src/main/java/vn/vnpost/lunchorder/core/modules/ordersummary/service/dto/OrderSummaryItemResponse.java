package vn.vnpost.lunchorder.core.modules.ordersummary.service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryItemResponse {
    private Long userId;
    private String fullName;
    private String departmentName;
    private Integer normalMealCount;
    private Integer specialMealCount;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
}
