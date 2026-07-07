package vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto;

import lombok.*;

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
    private java.math.BigDecimal price;

    public Boolean getIsSpecial() {
        return price != null && price.compareTo(new java.math.BigDecimal("25000")) > 0;
    }
    private String status;
    private Instant createdAt;
    private Long buyerId;
    private String buyerName;
}
