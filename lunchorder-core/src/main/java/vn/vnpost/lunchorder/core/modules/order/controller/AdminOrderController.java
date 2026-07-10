package vn.vnpost.lunchorder.core.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.order.service.OrderService;
import vn.vnpost.lunchorder.core.modules.order.service.dto.AdminOrderListResponse;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderResponse;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderStatusUpdateRequest;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderTransferRequest;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_ORDERS')")
    public ApiResponse<AdminOrderListResponse> getOrders(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.<AdminOrderListResponse>builder()
                .result(orderService.getAdminOrders(date, status))
                .build();
    }

    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('OVERRIDE_ORDERS')")
    public ApiResponse<OrderResponse> transfer(
            @PathVariable Long id,
            @RequestBody @Valid OrderTransferRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.transferOrder(id, request))
                .build();
    }

    @PutMapping("/{id}/print")
    @PreAuthorize("hasAuthority('MANAGE_ORDERS')")
    public ApiResponse<OrderResponse> print(
            @PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.printOrder(id))
                .build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_ORDERS')")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid OrderStatusUpdateRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.updateOrderStatus(id, request))
                .build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ApiResponse<java.util.List<OrderResponse>> getUserOrdersInPeriod(
            @PathVariable Long userId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.<java.util.List<OrderResponse>>builder()
                .result(orderService.getMyOrders(userId, fromDate, toDate))
                .build();
    }
}
