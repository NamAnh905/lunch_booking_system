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

import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketSource;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;

import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final TicketExchangeRepository ticketExchangeRepository;
    private final OrderMapper orderMapper;

    private boolean isCutOffReached(LocalDate menuDate) {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = menuDate.minusDays(1);
        if (today.isAfter(cutoffDate)) {
            return true;
        }
        if (today.isEqual(cutoffDate)) {
            return LocalTime.now().isAfter(getCutOffTime());
        }
        return false;
    }

    private LocalTime getCutOffTime() {
        return systemConfigRepository.findByConfigKey("CUT_OFF_TIME")
                .map(config -> {
                    try {
                        return LocalTime.parse(config.getConfigValue());
                    } catch (Exception e) {
                        log.error("Failed to parse CUT_OFF_TIME configuration: {}", config.getConfigValue(), e);
                        return LocalTime.of(14, 45);
                    }
                })
                .orElse(LocalTime.of(14, 45));
    }

    @Override
    public List<OrderResponse> getMyOrders(Long userId, LocalDate fromDate, LocalDate toDate) {
        List<Order> orders = orderRepository.findByUserIdAndOrderDateBetween(userId, fromDate, toDate);
        return orderMapper.toDtoList(orders);
    }

    @Override
    @Transactional
    public List<OrderResponse> createOrders(Long userId, OrderCreateRequest request) {
        log.info("OrderService processing createOrders for user ID {}: payload = {}", userId, request.getOrderDates());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<OrderResponse> responses = new ArrayList<>();
        for (LocalDate orderDate : request.getOrderDates()) {
            try {
                // Check time constraints (cutoff time is day before menu date)
                if (isCutOffReached(orderDate)) {
                    throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
                }

                // Check if user already ordered a meal for this date
                Optional<Order> existingOrderOpt = orderRepository.findByUserIdAndOrderDate(userId, orderDate);
                if (existingOrderOpt.isPresent()) {
                    Order existingOrder = existingOrderOpt.get();
                    if (OrderStatus.CANCELLED.name().equalsIgnoreCase(existingOrder.getStatus())) {
                        List<Menu> menus = menuRepository.findByMenuDate(orderDate);
                        Menu menu = menus.isEmpty() ? null : menus.get(0);

                        // Upsert: Reactivate the cancelled order instead of inserting a new record
                        existingOrder.setStatus(OrderStatus.PENDING.name());
                        existingOrder.setMenu(menu);
                        existingOrder.setPrice(
                                menu != null ? menu.getPrice().getAmount() : java.math.BigDecimal.valueOf(25000));
                        existingOrder.setTicketSource(TicketSource.STANDARD.name());
                        existingOrder.setIsPrinted(false);
                        existingOrder.setOriginalUser(user);
                        existingOrder = orderRepository.save(existingOrder);
                        responses.add(orderMapper.toDto(existingOrder));
                        continue;
                    } else {
                        throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
                    }
                }

                List<Menu> menus = menuRepository.findByMenuDate(orderDate);
                Menu menu = menus.isEmpty() ? null : menus.get(0);

                Order order = new Order();
                order.setUser(user);
                order.setOrderDate(orderDate);
                order.setMenu(menu);
                order.setPrice(menu != null ? menu.getPrice().getAmount() : java.math.BigDecimal.valueOf(25000));
                order.setStatus(OrderStatus.PENDING.name());
                order.setTicketSource(TicketSource.STANDARD.name());
                order.setOriginalUser(user);
                order.setIsPrinted(false);

                order = orderRepository.save(order);
                responses.add(orderMapper.toDto(order));
            } catch (AppException e) {
                OrderResponse failedResponse = new OrderResponse();
                failedResponse.setStatus("FAILED");
                failedResponse.setErrorMessage(e.getErrorCode().getMessage());
                failedResponse.setMenuDate(orderDate);
                responses.add(failedResponse);
            } catch (Exception e) {
                OrderResponse failedResponse = new OrderResponse();
                failedResponse.setStatus("FAILED");
                failedResponse.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
                failedResponse.setMenuDate(orderDate);
                responses.add(failedResponse);
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        // Prevent cancellation if the ticket is listed in the market
        if (ticketExchangeRepository.findByOrderIdAndStatus(orderId, TicketExchangeStatus.OPEN.name()).isPresent()) {
            throw new AppException(ErrorCode.ORDER_IN_MARKET);
        }

        // Check time constraints (cutoff time is day before menu date)
        if (isCutOffReached(order.getOrderDate())) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
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

        // Check if target user already ordered a meal for this date
        if (orderRepository.findByUserIdAndOrderDate(request.getTargetUserId(), order.getOrderDate()).isPresent()) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        order.setUser(targetUser);
        order.setStatus(OrderStatus.TRANSFERRED.name());

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
