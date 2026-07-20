package vn.vnpost.lunchorder.core.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.order.service.OrderService;
import vn.vnpost.lunchorder.core.modules.order.service.dto.DepartmentMemberOrderResponse;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderCreateRequest;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderResponse;
import vn.vnpost.lunchorder.common.security.CurrentUserId;

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
                        @CurrentUserId Long userId,
                        @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
                return ApiResponse.<List<OrderResponse>>builder()
                                .result(orderService.getOrdersByUser(userId, fromDate, toDate))
                                .build();
        }

        @GetMapping("/department-today")
        @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
        public ApiResponse<List<DepartmentMemberOrderResponse>> getDepartmentToday(
                        @CurrentUserId Long userId) {
                return ApiResponse.<List<DepartmentMemberOrderResponse>>builder()
                                .result(orderService.getDepartmentMealListToday(userId))
                                .build();
        }

        @PostMapping
        @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
        public ApiResponse<List<OrderResponse>> create(
                        @CurrentUserId Long userId,
                        @RequestBody @Valid OrderCreateRequest request) {
                log.debug("Received request to create orders for user ID {}: orders = {}", userId,
                                request.getOrders());
                return ApiResponse.<List<OrderResponse>>builder()
                                .result(orderService.createOrders(userId, request))
                                .build();
        }

        @PatchMapping("/{id}/cancel")
        @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
        public ApiResponse<OrderResponse> cancel(
                        @CurrentUserId Long userId,
                        @PathVariable Long id) {
                return ApiResponse.<OrderResponse>builder()
                                .result(orderService.cancelOrder(userId, id))
                                .build();
        }
}
