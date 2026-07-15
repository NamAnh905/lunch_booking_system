package vn.vnpost.lunchorder.core.modules.order.service;

import vn.vnpost.lunchorder.core.modules.order.service.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    List<OrderResponse> getOrdersByUser(Long userId, LocalDate fromDate, LocalDate toDate);

    List<OrderResponse> createOrders(Long userId, OrderCreateRequest request);

    OrderResponse cancelOrder(Long userId, Long orderId);

    AdminOrderListResponse getAdminOrders(LocalDate date, String status);

    OrderResponse transferOrder(Long orderId, OrderTransferRequest request);

    OrderResponse printOrder(Long orderId);

    OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);
}
