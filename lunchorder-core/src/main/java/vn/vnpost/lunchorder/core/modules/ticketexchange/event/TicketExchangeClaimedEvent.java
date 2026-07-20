package vn.vnpost.lunchorder.core.modules.ticketexchange.event;

import java.time.LocalDate;

public record TicketExchangeClaimedEvent(
        Long sellerId,
        String sellerName,
        Long buyerId,
        String buyerName,
        LocalDate orderDate) {
}
