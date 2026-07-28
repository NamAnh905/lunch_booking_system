package vn.vnpost.lunchorder.core.modules.order.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.menu.entity.Menu;
import vn.vnpost.lunchorder.core.modules.order.entity.Order;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.menu.repository.MenuRepository;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.core.modules.order.service.OrderService;
import vn.vnpost.lunchorder.core.modules.order.service.dto.*;
import vn.vnpost.lunchorder.core.modules.order.service.mapstruct.OrderMapper;
import vn.vnpost.lunchorder.system.modules.user.service.UserLookupService;

import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketSource;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;

import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.modules.price.service.MealPricePolicy;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final UserLookupService userLookupService;
    private final TicketExchangeRepository ticketExchangeRepository;
    private final OrderMapper orderMapper;
    private final MealPricePolicy mealPricePolicy;
    private final CutOffPolicy cutOffPolicy;
    private final OrderItemPersister orderItemPersister;

    @Override
    public List<OrderResponse> getOrdersByUser(Long userId, LocalDate fromDate, LocalDate toDate) {
        List<Order> orders = orderRepository.findByUserIdAndOrderDateBetween(userId, fromDate, toDate, TicketExchangeStatus.OPEN);
        return orderMapper.toDtoList(orders);
    }

    @Override
    public List<DepartmentMemberOrderResponse> getDepartmentMealListToday(Long userId) {
        User user = userLookupService.getById(userId);
        if (user.getDepartment() == null) {
            return List.of();
        }
        return orderRepository.findDepartmentMealListByDate(
                user.getDepartment().getId(), LocalDate.now(), OrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public List<OrderResponse> createOrders(Long userId, OrderCreateRequest request) {
        User user = userLookupService.getById(userId);

        Set<LocalDate> holidays = cutOffPolicy.getHolidayDates();
        LocalDate maxOrderableDate = cutOffPolicy.getMaxOrderableDate();

        List<OrderResponse> responses = new ArrayList<>();
        for (OrderItemRequest item : request.getOrders()) {
            LocalDate orderDate = item.getOrderDate();
            boolean isSpecial = Boolean.TRUE.equals(item.getIsSpecial());

            try {
                if (cutOffPolicy.isWeekend(orderDate) || holidays.contains(orderDate)) {
                    throw new AppException(ErrorCode.ORDER_DATE_NOT_ALLOWED);
                }

                if (orderDate.isAfter(maxOrderableDate)) {
                    throw new AppException(ErrorCode.ORDER_DATE_TOO_FAR);
                }

                if (cutOffPolicy.isCutOffReached(orderDate)) {
                    throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
                }

                Menu menu = resolveMenu(orderDate, isSpecial);

                Optional<Order> existingOrderOpt = orderRepository.findByUserIdAndOrderDate(userId, orderDate);
                if (existingOrderOpt.isPresent()) {
                    Order existingOrder = existingOrderOpt.get();
                    if (existingOrder.getStatus() == OrderStatus.CANCELLED) {
                        existingOrder.setStatus(OrderStatus.PENDING);
                        existingOrder.setMenu(menu);
                        existingOrder.setPrice(resolveOrderPrice(menu, isSpecial));
                        existingOrder.setTicketSource(TicketSource.STANDARD);
                        existingOrder.setIsPrinted(false);
                        existingOrder.setOriginalUser(user);
                        responses.add(orderMapper.toDto(existingOrder));
                        continue;
                    } else {
                        throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
                    }
                }

                Order order = new Order();
                order.setUser(user);
                order.setOrderDate(orderDate);
                order.setMenu(menu);
                order.setPrice(resolveOrderPrice(menu, isSpecial));
                order.setStatus(OrderStatus.PENDING);
                order.setTicketSource(TicketSource.STANDARD);
                order.setOriginalUser(user);
                order.setIsPrinted(false);

                order = orderItemPersister.persist(order);
                responses.add(orderMapper.toDto(order));
            } catch (AppException e) {
                OrderResponse failedResponse = new OrderResponse();
                failedResponse.setStatus("FAILED");
                failedResponse.setErrorMessage(e.getErrorCode().getMessage());
                failedResponse.setMenuDate(orderDate);
                responses.add(failedResponse);
            }
        }
        return responses;
    }

    private Menu resolveMenu(LocalDate orderDate, boolean isSpecial) {
        BigDecimal targetAmount = mealPricePolicy.resolvePrice(isSpecial);

        return menuRepository.findByMenuDateAndPrice_Amount(orderDate, targetAmount)
                .orElseGet(() -> {
                    List<Menu> menus = menuRepository.findByMenuDate(orderDate);
                    return menus.isEmpty() ? null : menus.get(0);
                });
    }

    private BigDecimal resolveOrderPrice(Menu menu, boolean isSpecial) {
        if (menu != null && menu.getPrice() != null && menu.getPrice().getAmount() != null) {
            return menu.getPrice().getAmount();
        }
        return mealPricePolicy.resolvePrice(isSpecial);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        if (ticketExchangeRepository.findByOrderIdAndStatus(orderId, TicketExchangeStatus.OPEN).isPresent()) {
            throw new AppException(ErrorCode.ORDER_IN_MARKET);
        }

        if (cutOffPolicy.isCutOffReached(order.getOrderDate())) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Override
    public AdminOrderListResponse getAdminOrders(LocalDate date, String status) {
        List<Order> orders = orderRepository.findByDateAndStatus(date, parseStatusOrNull(status));
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

        User targetUser = userLookupService.getById(request.getTargetUserId());

        if (orderRepository.findByUserIdAndOrderDate(request.getTargetUserId(), order.getOrderDate()).isPresent()) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        order.setUser(targetUser);
        order.setStatus(OrderStatus.TRANSFERRED);

        order = orderItemPersister.persist(order);
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

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_ENUM_VALUE);
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    private OrderStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ENUM_VALUE);
        }
    }
}
