package vn.vnpost.lunchorder.core.modules.ticketexchange.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeCreateRequest;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;
import vn.vnpost.lunchorder.common.security.CurrentUserId;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/tickets/market")
public class TicketExchangeController {

        private final TicketExchangeService ticketExchangeService;

        @GetMapping
        @PreAuthorize("hasAuthority('CREATE_TICKET')")
        public ApiResponse<PageResponse<TicketExchangeResponse>> getMarketTickets(
                        @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
                        @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
                        @RequestParam(name = "keyword", required = false) String keyword) {
                PageResponse<TicketExchangeResponse> openExchanges = ticketExchangeService.getOpenExchanges(
                                page, size, keyword);
                return ApiResponse.<PageResponse<TicketExchangeResponse>>builder()
                                .result(openExchanges)
                                .build();
        }

        @PostMapping
        @PreAuthorize("hasAuthority('CREATE_TICKET')")
        public ApiResponse<TicketExchangeResponse> postTicket(
                        @CurrentUserId Long userId,
                        @RequestBody @Valid TicketExchangeCreateRequest request) {
                TicketExchangeResponse response = ticketExchangeService.postTicketToMarket(userId,
                                request);
                return ApiResponse.<TicketExchangeResponse>builder()
                                .result(response)
                                .build();
        }

        @DeleteMapping("/{exchangeId}")
        @PreAuthorize("hasAuthority('CREATE_TICKET')")
        public ApiResponse<String> withdrawTicket(
                        @CurrentUserId Long userId,
                        @PathVariable Long exchangeId) {
                ticketExchangeService.withdrawTicketFromMarket(userId, exchangeId);
                return ApiResponse.<String>builder()
                                .result("Withdraw market ticket success")
                                .build();
        }

        @PostMapping("/{exchangeId}/claim")
        @PreAuthorize("hasAuthority('CLAIM_TICKET')")
        public ApiResponse<TicketExchangeResponse> claimTicket(
                        @CurrentUserId Long userId,
                        @PathVariable Long exchangeId) {
                TicketExchangeResponse response = ticketExchangeService.claimTicket(userId,
                                exchangeId);
                return ApiResponse.<TicketExchangeResponse>builder()
                                .result(response)
                                .build();
        }

        @GetMapping("/my-tickets")
        @PreAuthorize("hasAuthority('CREATE_TICKET')")
        public ApiResponse<List<TicketExchangeResponse>> getMyListedTickets(
                        @CurrentUserId Long userId) {
                List<TicketExchangeResponse> myList = ticketExchangeService.getMyListedTickets(userId);
                return ApiResponse.<List<TicketExchangeResponse>>builder()
                                .result(myList)
                                .build();
        }
}
