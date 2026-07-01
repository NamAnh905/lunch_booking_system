package vn.vnpost.lunchorder.core.modules.ticketexchange.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/tickets/exchanges")
public class AdminTicketExchangeController {

    private final TicketExchangeService ticketExchangeService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_ORDERS')")
    public ApiResponse<List<TicketExchangeResponse>> getExchanges(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "status", required = false) String status) {
        List<TicketExchangeResponse> result = ticketExchangeService.getAdminExchanges(startDate, status);
        return ApiResponse.<List<TicketExchangeResponse>>builder()
                .result(result)
                .build();
    }
}
