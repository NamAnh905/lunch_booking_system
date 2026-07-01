package vn.vnpost.lunchorder.core.modules.ticketexchange.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeCreateRequest;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tickets/market")
public class TicketExchangeController {

        private final TicketExchangeService ticketExchangeService;

        @GetMapping
        @PreAuthorize("hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<PageResponse<TicketExchangeResponse>> getMarketTickets(
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size) {
                PageResponse<TicketExchangeResponse> openExchanges = ticketExchangeService.getOpenExchanges(page, size);
                return ApiResponse.<PageResponse<TicketExchangeResponse>>builder()
                                .result(openExchanges)
                                .build();
        }

        @PostMapping
        @PreAuthorize("hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<TicketExchangeResponse> postTicket(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @RequestBody @Valid TicketExchangeCreateRequest request) {
                TicketExchangeResponse response = ticketExchangeService.postTicketToMarket(userPrincipal.getUserId(),
                                request);
                return ApiResponse.<TicketExchangeResponse>builder()
                                .result(response)
                                .build();
        }

        @DeleteMapping("/{exchangeId}")
        @PreAuthorize("hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<String> withdrawTicket(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @PathVariable Long exchangeId) {
                ticketExchangeService.withdrawTicketFromMarket(userPrincipal.getUserId(), exchangeId);
                return ApiResponse.<String>builder()
                                .result("Withdraw market ticket success")
                                .build();
        }

        @PostMapping("/{exchangeId}/claim")
        @PreAuthorize("hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<TicketExchangeResponse> claimTicket(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @PathVariable Long exchangeId) {
                TicketExchangeResponse response = ticketExchangeService.claimTicket(userPrincipal.getUserId(),
                                exchangeId);
                return ApiResponse.<TicketExchangeResponse>builder()
                                .result(response)
                                .build();
        }
}
