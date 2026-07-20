package vn.vnpost.lunchorder.core.modules.ticketexchange.event;

import java.time.LocalDate;

public record TicketExchangeExpiredEvent(Long ownerId, LocalDate orderDate) {
}
