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

import vn.vnpost.lunchorder.common.enums.MealType;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketSource;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;

import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.modules.price.service.MealPricePolicy;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;
import vn.vnpost.lunchorder.core.policy.OrderableDates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final String FAILED_ITEM_STATUS = "FAILED";

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
                user.getDepartment().getId(), cutOffPolicy.today(), OrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public List<OrderResponse> createOrders(Long userId, OrderCreateRequest request) {
        User user = userLookupService.getById(userId);
        OrderableDates orderableDates = OrderableDates.snapshot(cutOffPolicy);
        MealPrices mealPrices = MealPrices.snapshot(mealPricePolicy);

        return request.getOrders().stream()
                .map(item -> createSingleOrder(user, item, orderableDates, mealPrices))
                .toList();
    }

    private OrderResponse createSingleOrder(User user, OrderItemRequest item,
            OrderableDates orderableDates, MealPrices mealPrices) {
        try {
            orderableDates.assertOrderable(item.getOrderDate());
            return orderMapper.toDto(saveOrder(user, item, mealPrices));
        } catch (AppException e) {
            return failedResponse(item.getOrderDate(), e.getErrorCode().getMessage());
        }
    }

    private OrderResponse failedResponse(LocalDate orderDate, String errorMessage) {
        OrderResponse response = new OrderResponse();
        response.setStatus(FAILED_ITEM_STATUS);
        response.setErrorMessage(errorMessage);
        response.setMenuDate(orderDate);
        return response;
    }

    private Order saveOrder(User user, OrderItemRequest item, MealPrices mealPrices) {
        LocalDate orderDate = item.getOrderDate();
        MealType requestedType = Boolean.TRUE.equals(item.getIsSpecial()) ? MealType.SPECIAL : MealType.NORMAL;

        Menu menu = resolveMenu(orderDate, requestedType);
        MealType mealType = resolveMealType(menu, requestedType);
        BigDecimal price = resolveOrderPrice(menu, mealType, mealPrices);

        return orderRepository.findByUserIdAndOrderDate(user.getId(), orderDate)
                .map(existing -> reactivate(existing, user, menu, mealType, price))
                .orElseGet(() -> orderItemPersister.persist(newOrder(user, orderDate, menu, mealType, price)));
    }

    private Order reactivate(Order existing, User user, Menu menu, MealType mealType, BigDecimal price) {
        if (existing.getStatus() != OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        existing.setStatus(OrderStatus.PENDING);
        existing.setMenu(menu);
        existing.setMealType(mealType);
        existing.setPrice(price);
        existing.setTicketSource(TicketSource.STANDARD);
        existing.setIsPrinted(false);
        existing.setOriginalUser(user);
        return existing;
    }

    private Order newOrder(User user, LocalDate orderDate, Menu menu, MealType mealType, BigDecimal price) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(orderDate);
        order.setMenu(menu);
        order.setMealType(mealType);
        order.setPrice(price);
        order.setStatus(OrderStatus.PENDING);
        order.setTicketSource(TicketSource.STANDARD);
        order.setOriginalUser(user);
        order.setIsPrinted(false);
        return order;
    }

    private Menu resolveMenu(LocalDate orderDate, MealType requestedType) {
        return menuRepository.findByMenuDateAndPrice_MealType(orderDate, requestedType)
                .orElseGet(() -> {
                    List<Menu> menus = menuRepository.findByMenuDate(orderDate);
                    return menus.isEmpty() ? null : menus.get(0);
                });
    }

    private MealType resolveMealType(Menu menu, MealType requestedType) {
        if (menu != null && menu.getPrice() != null && menu.getPrice().getMealType() != null) {
            return menu.getPrice().getMealType();
        }
        return requestedType;
    }

    private BigDecimal resolveOrderPrice(Menu menu, MealType mealType, MealPrices mealPrices) {
        if (menu != null && menu.getPrice() != null && menu.getPrice().getAmount() != null) {
            return menu.getPrice().getAmount();
        }
        return mealPrices.forType(mealType);
    }

    private record MealPrices(BigDecimal normal, BigDecimal special) {

        static MealPrices snapshot(MealPricePolicy mealPricePolicy) {
            return new MealPrices(mealPricePolicy.getNormalPrice(), mealPricePolicy.getSpecialPrice());
        }

        BigDecimal forType(MealType mealType) {
            return mealType == MealType.SPECIAL ? special : normal;
        }
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
