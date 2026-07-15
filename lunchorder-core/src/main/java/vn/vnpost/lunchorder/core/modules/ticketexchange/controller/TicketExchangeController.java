package vn.vnpost.lunchorder.core.modules.ticketexchange.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeCreateRequest;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/tickets/market")
public class TicketExchangeController {

        private final TicketExchangeService ticketExchangeService;

        @GetMapping
        @PreAuthorize("hasRole('USER') and hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<PageResponse<TicketExchangeResponse>> getMarketTickets(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
                        @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
                        @RequestParam(name = "keyword", required = false) String keyword) {
                PageResponse<TicketExchangeResponse> openExchanges = ticketExchangeService.getOpenExchanges(
                                userPrincipal.getUserId(), page, size, keyword);
                return ApiResponse.<PageResponse<TicketExchangeResponse>>builder()
                                .result(openExchanges)
                                .build();
        }

        @PostMapping
        @PreAuthorize("hasRole('USER') and hasAuthority('EXCHANGE_TICKETS')")
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
        @PreAuthorize("hasRole('USER') and hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<String> withdrawTicket(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @PathVariable Long exchangeId) {
                ticketExchangeService.withdrawTicketFromMarket(userPrincipal.getUserId(), exchangeId);
                return ApiResponse.<String>builder()
                                .result("Withdraw market ticket success")
                                .build();
        }

        @PostMapping("/{exchangeId}/claim")
        @PreAuthorize("hasRole('USER') and hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<TicketExchangeResponse> claimTicket(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @PathVariable Long exchangeId) {
                TicketExchangeResponse response = ticketExchangeService.claimTicket(userPrincipal.getUserId(),
                                exchangeId);
                return ApiResponse.<TicketExchangeResponse>builder()
                                .result(response)
                                .build();
        }

        @GetMapping("/my-tickets")
        @PreAuthorize("hasRole('USER') and hasAuthority('EXCHANGE_TICKETS')")
        public ApiResponse<List<TicketExchangeResponse>> getMyListedTickets(
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {
                List<TicketExchangeResponse> myList = ticketExchangeService.getMyListedTickets(userPrincipal.getUserId());
                return ApiResponse.<List<TicketExchangeResponse>>builder()
                                .result(myList)
                                .build();
        }
}
