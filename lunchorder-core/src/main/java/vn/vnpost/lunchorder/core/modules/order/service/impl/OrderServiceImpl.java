package vn.vnpost.lunchorder.core.modules.order.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.entity.Menu;
import vn.vnpost.lunchorder.common.entity.Order;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.menu.repository.MenuRepository;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.core.modules.order.service.OrderService;
import vn.vnpost.lunchorder.core.modules.order.service.dto.*;
import vn.vnpost.lunchorder.core.modules.order.service.mapstruct.OrderMapper;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    private static final LocalTime CUT_OFF_TIME = LocalTime.of(9, 0);

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId, LocalDate fromDate, LocalDate toDate) {
        List<Order> orders = orderRepository.findByUserIdAndMenuMenuDateBetween(userId, fromDate, toDate);
        return orderMapper.toDtoList(orders);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Menu menu = menuRepository.findById(request.getMenuId())
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));

        // Check if user already ordered a meal for this date (via menu)
        if (orderRepository.findByUserIdAndMenuId(userId, request.getMenuId()).isPresent()) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        // Check time constraints (cutoff time 9:00 AM on the day of the menu)
        LocalDate menuDate = menu.getMenuDate();
        LocalDate today = LocalDate.now();
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        } else if (menuDate.isEqual(today)) {
            if (LocalTime.now().isAfter(CUT_OFF_TIME)) {
                throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setMenu(menu);
        order.setPrice(menu.getPrice());
        order.setStatus("PENDING");
        order.setTicketSource("STANDARD");
        order.setOriginalUser(user);
        order.setIsPrinted(false);

        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // Check time constraints
        LocalDate menuDate = order.getMenu().getMenuDate();
        LocalDate today = LocalDate.now();
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        } else if (menuDate.isEqual(today)) {
            if (LocalTime.now().isAfter(CUT_OFF_TIME)) {
                throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
            }
        }

        order.setStatus("CANCELLED");
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderListResponse getAdminOrders(LocalDate date, String status) {
        List<Order> orders = orderRepository.findByDateAndStatus(date, status);
        List<OrderResponse> dtoList = orderMapper.toDtoList(orders);
        return AdminOrderListResponse.builder()
                .totalCount(dtoList.size())
                .orders(dtoList)
                .build();
        }

    @Override
    @Transactional
    public OrderResponse transferOrder(Long orderId, OrderTransferRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if target user already ordered a meal for this date (via menu)
        if (orderRepository.findByUserIdAndMenuId(request.getTargetUserId(), order.getMenu().getId()).isPresent()) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        order.setUser(targetUser);
        order.setStatus("TRANSFERRED");
        
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderResponse printOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        order.setIsPrinted(true);
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        order.setStatus(request.getStatus().toUpperCase());
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }
}
