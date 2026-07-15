package vn.vnpost.lunchorder.core.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.order.service.OrderService;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderCreateRequest;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderResponse;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/orders")
public class OrderController {

        private final OrderService orderService;

        @GetMapping("/me")
        @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
        public ApiResponse<List<OrderResponse>> getMyOrders(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
                return ApiResponse.<List<OrderResponse>>builder()
                                .result(orderService.getOrdersByUser(userPrincipal.getUserId(), fromDate, toDate))
                                .build();
        }

        @PostMapping
        @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
        public ApiResponse<List<OrderResponse>> create(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @RequestBody @Valid OrderCreateRequest request) {
                log.debug("Received request to create orders for user ID {}: orders = {}", userPrincipal.getUserId(),
                                request.getOrders());
                return ApiResponse.<List<OrderResponse>>builder()
                                .result(orderService.createOrders(userPrincipal.getUserId(), request))
                                .build();
        }

        @PatchMapping("/{id}/cancel")
        @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
        public ApiResponse<OrderResponse> cancel(
                        @AuthenticationPrincipal UserPrincipal userPrincipal,
                        @PathVariable Long id) {
                return ApiResponse.<OrderResponse>builder()
                                .result(orderService.cancelOrder(userPrincipal.getUserId(), id))
                                .build();
        }
}
