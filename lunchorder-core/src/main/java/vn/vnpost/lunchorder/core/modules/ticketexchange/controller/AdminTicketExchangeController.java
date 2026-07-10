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

import vn.vnpost.lunchorder.common.base.PageResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/tickets/exchanges")
public class AdminTicketExchangeController {

    private final TicketExchangeService ticketExchangeService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_ORDERS')")
    public ApiResponse<PageResponse<TicketExchangeResponse>> getExchanges(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword) {
        PageResponse<TicketExchangeResponse> result = ticketExchangeService.getAdminExchanges(page, size, startDate, endDate, status, keyword);
        return ApiResponse.<PageResponse<TicketExchangeResponse>>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/{exchangeId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDERS')") // Assume admin has this
    public ApiResponse<String> forceCancelTicket(@PathVariable Long exchangeId) {
        ticketExchangeService.forceCancelTicket(exchangeId);
        return ApiResponse.<String>builder()
                .result("Huỷ vé thành công")
                .build();
    }
}
