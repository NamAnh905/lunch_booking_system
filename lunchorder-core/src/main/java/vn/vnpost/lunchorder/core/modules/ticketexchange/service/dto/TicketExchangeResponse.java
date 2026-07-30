package vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto;

import lombok.*;
import vn.vnpost.lunchorder.common.enums.MealType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketExchangeResponse {
    private Long exchangeId;
    private Long orderId;
    private String sellerName;
    private Long sellerId;
    private LocalDate menuDate;
    private BigDecimal price;
    private MealType mealType;
    private Boolean isSpecial;
    private String status;
    private Instant createdAt;
    private Long buyerId;
    private String buyerName;
}
