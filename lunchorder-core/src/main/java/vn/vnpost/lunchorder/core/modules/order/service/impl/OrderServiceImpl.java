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
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId, LocalDate fromDate, LocalDate toDate) {
        List<Order> orders = orderRepository.findByUserIdAndMenuMenuDateBetween(userId, fromDate, toDate);
        return orderMapper.toDtoList(orders);
    }

    @Override
    @Transactional
    public List<OrderResponse> createOrders(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<OrderResponse> responses = new ArrayList<>();
        for (Long menuId : request.getMenuIds()) {
            try {
                Menu menu = menuRepository.findById(menuId)
                        .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));

                // Check time constraints (cutoff time is day before menu date)
                if (isCutOffReached(menu.getMenuDate())) {
                    throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
                }

                // Check if user already ordered a meal for this date (via menu)
                Optional<Order> existingOrderOpt = orderRepository.findByUserIdAndMenuId(userId, menuId);
                if (existingOrderOpt.isPresent()) {
                    Order existingOrder = existingOrderOpt.get();
                    if (OrderStatus.CANCELLED.name().equalsIgnoreCase(existingOrder.getStatus())) {
                        // Check if there is another active order on the same day
                        final Long existingOrderId = existingOrder.getId();
                        boolean hasActiveOrderOnSameDay = orderRepository
                                .findByUserIdAndMenuMenuDateBetween(userId, menu.getMenuDate(), menu.getMenuDate())
                                .stream()
                                .anyMatch(o -> !OrderStatus.CANCELLED.name().equalsIgnoreCase(o.getStatus())
                                        && !o.getId().equals(existingOrderId));
                        if (hasActiveOrderOnSameDay) {
                            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
                        }

                        // Upsert: Reactivate the cancelled order instead of inserting a new record
                        existingOrder.setStatus(OrderStatus.PENDING.name());
                        existingOrder.setPrice(menu.getPrice());
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

                // Check if there is any active order for any menu on this day
                boolean hasActiveOrderOnSameDay = orderRepository
                        .findByUserIdAndMenuMenuDateBetween(userId, menu.getMenuDate(), menu.getMenuDate()).stream()
                        .anyMatch(o -> !OrderStatus.CANCELLED.name().equalsIgnoreCase(o.getStatus()));
                if (hasActiveOrderOnSameDay) {
                    throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
                }

                Order order = new Order();
                order.setUser(user);
                order.setMenu(menu);
                order.setPrice(menu.getPrice());
                order.setStatus(OrderStatus.PENDING.name());
                order.setTicketSource(TicketSource.STANDARD.name());
                order.setOriginalUser(user);
                order.setIsPrinted(false);

                order = orderRepository.save(order);
                responses.add(orderMapper.toDto(order));
            } catch (AppException e) {
                OrderResponse failedResponse = new OrderResponse();
                failedResponse.setMenuId(menuId);
                failedResponse.setStatus("FAILED");
                failedResponse.setErrorMessage(e.getErrorCode().getMessage());
                try {
                    menuRepository.findById(menuId).ifPresent(m -> failedResponse.setMenuDate(m.getMenuDate()));
                } catch (Exception ignored) {
                }
                responses.add(failedResponse);
            } catch (Exception e) {
                OrderResponse failedResponse = new OrderResponse();
                failedResponse.setMenuId(menuId);
                failedResponse.setStatus("FAILED");
                failedResponse.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
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
        if (isCutOffReached(order.getMenu().getMenuDate())) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        order.setStatus(OrderStatus.CANCELLED.name());
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
