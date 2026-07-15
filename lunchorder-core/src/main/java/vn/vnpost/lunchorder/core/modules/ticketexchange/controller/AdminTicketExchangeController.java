package vn.vnpost.lunchorder.core.modules.ticketexchange.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;

import java.time.LocalDate;

import vn.vnpost.lunchorder.common.base.PageResponse;

/**
 * Read-only administrative view over the ticket marketplace ("chợ pass vé").
 *
 * <p>By business rule, administrators may only inspect active and historical
 * ticket listings; they must never post, claim, withdraw, or otherwise mutate
 * exchange entries. All write operations on the marketplace belong exclusively
 * to regular users and live in {@link TicketExchangeController}.
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/tickets/exchanges")
public class AdminTicketExchangeController {

    private final TicketExchangeService ticketExchangeService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_ORDERS')")
    public ApiResponse<PageResponse<TicketExchangeResponse>> getExchanges(
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword) {
        PageResponse<TicketExchangeResponse> result = ticketExchangeService.getAdminExchanges(page, size, startDate, endDate, status, keyword);
        return ApiResponse.<PageResponse<TicketExchangeResponse>>builder()
                .result(result)
                .build();
    }
}
