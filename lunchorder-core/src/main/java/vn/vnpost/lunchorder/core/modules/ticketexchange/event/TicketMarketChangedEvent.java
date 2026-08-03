package vn.vnpost.lunchorder.core.modules.ticketexchange.event;

/**
 * Signals that the set of tickets on the market changed. Carries no ticket data
 * on purpose: every viewer has their own page, keyword and permissions, so each
 * client re-queries the market itself instead of the server rebuilding a list
 * per viewer.
 */
public record TicketMarketChangedEvent(TicketMarketChangeReason reason) {
}
