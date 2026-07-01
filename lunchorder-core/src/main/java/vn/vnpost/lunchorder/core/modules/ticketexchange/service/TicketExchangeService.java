package vn.vnpost.lunchorder.core.modules.ticketexchange.service;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeCreateRequest;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;

import java.time.LocalDate;
import java.util.List;

public interface TicketExchangeService {
    PageResponse<TicketExchangeResponse> getOpenExchanges(int page, int size);
    TicketExchangeResponse postTicketToMarket(Long userId, TicketExchangeCreateRequest request);
    void withdrawTicketFromMarket(Long userId, Long exchangeId);
    TicketExchangeResponse claimTicket(Long userId, Long exchangeId);
    List<TicketExchangeResponse> getAdminExchanges(LocalDate startDate, String status);
}
