package vn.vnpost.lunchorder.core.modules.ticketexchange.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Order;
import vn.vnpost.lunchorder.common.entity.TicketExchange;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.common.repository.SystemConfigRepository;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeCreateRequest;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.mapstruct.TicketExchangeMapper;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;

import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.enums.TicketExchangeStatus;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketExchangeServiceImpl implements TicketExchangeService {

    private final TicketExchangeRepository ticketExchangeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final TicketExchangeMapper ticketExchangeMapper;
    private final NotificationService notificationService;

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
    public PageResponse<TicketExchangeResponse> getOpenExchanges(int page, int size) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<TicketExchange> entityPage = ticketExchangeRepository.findByStatus(TicketExchangeStatus.OPEN.name(), pageable);
        List<TicketExchangeResponse> dtoList = entityPage.getContent().stream()
                .map(ticketExchangeMapper::toDto)
                .toList();

        return PageResponse.<TicketExchangeResponse>builder()
                .currentPage(page)
                .totalPages(entityPage.getTotalPages())
                .pageSize(size)
                .totalElements(entityPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional
    public TicketExchangeResponse postTicketToMarket(Long userId, TicketExchangeCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Verify status is PENDING
        if (!OrderStatus.PENDING.name().equalsIgnoreCase(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANNOT_PASS);
        }

        // Verify order is not already listed in market
        if (ticketExchangeRepository.findByOrderIdAndStatus(order.getId(), TicketExchangeStatus.OPEN.name())
                .isPresent()) {
            throw new AppException(ErrorCode.ORDER_IN_MARKET);
        }

        // Check time constraints
        LocalDate menuDate = order.getMenu().getMenuDate();
        LocalDate today = LocalDate.now();

        // Rule 1: Cannot pass a ticket of a past menu date
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CANNOT_PASS);
        }

        // Rule 2: Cannot pass if the menu date is still editable for
        // ordering/cancelling
        if (!isCutOffReached(menuDate)) {
            throw new AppException(ErrorCode.ORDER_CANNOT_PASS);
        }

        // Check if there is a cancelled/withdrawn exchange for this order, we can
        // update it or create new
        Optional<TicketExchange> existingExchangeOpt = ticketExchangeRepository.findByOrderIdAndStatus(order.getId(),
                TicketExchangeStatus.CANCELLED.name());
        TicketExchange ticketExchange;
        if (existingExchangeOpt.isPresent()) {
            ticketExchange = existingExchangeOpt.get();
            ticketExchange.setStatus(TicketExchangeStatus.OPEN.name());
            ticketExchange.setBuyer(null);
        } else {
            ticketExchange = new TicketExchange();
            ticketExchange.setOrder(order);
            ticketExchange.setStatus(TicketExchangeStatus.OPEN.name());
        }

        ticketExchange = ticketExchangeRepository.save(ticketExchange);
        return ticketExchangeMapper.toDto(ticketExchange);
    }

    @Override
    @Transactional
    public void withdrawTicketFromMarket(Long userId, Long exchangeId) {
        TicketExchange ticketExchange = ticketExchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (!TicketExchangeStatus.OPEN.name().equalsIgnoreCase(ticketExchange.getStatus())) {
            throw new AppException(ErrorCode.EXCHANGE_NOT_OPEN);
        }

        // Verify ownership of the order
        if (!ticketExchange.getOrder().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Check time constraints: Cannot withdraw a ticket for a past menu date
        LocalDate menuDate = ticketExchange.getOrder().getMenu().getMenuDate();
        LocalDate today = LocalDate.now();
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        ticketExchange.setStatus(TicketExchangeStatus.CANCELLED.name());
        ticketExchangeRepository.save(ticketExchange);
    }

    @Override
    @Transactional
    public TicketExchangeResponse claimTicket(Long userId, Long exchangeId) {
        TicketExchange ticketExchange = ticketExchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (!TicketExchangeStatus.OPEN.name().equalsIgnoreCase(ticketExchange.getStatus())) {
            throw new AppException(ErrorCode.EXCHANGE_NOT_OPEN);
        }

        // Cannot claim own ticket
        if (ticketExchange.getOrder().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_CLAIM_OWN_TICKET);
        }

        // Check if buyer already has an active order for this menu date
        LocalDate menuDate = ticketExchange.getOrder().getMenu().getMenuDate();
        boolean hasActiveOrderOnSameDay = orderRepository.findByUserIdAndMenuMenuDateBetween(userId, menuDate, menuDate).stream()
                .anyMatch(o -> !OrderStatus.CANCELLED.name().equalsIgnoreCase(o.getStatus()));
        if (hasActiveOrderOnSameDay) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        // Check time constraints: Cannot claim a ticket for a past menu date
        LocalDate today = LocalDate.now();
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Save original owner for notification
        User seller = orderRepository.findById(ticketExchange.getOrder().getId())
                .map(Order::getUser)
                .orElse(ticketExchange.getOrder().getUser());

        // Update exchange
        ticketExchange.setStatus(TicketExchangeStatus.MATCHED.name());
        ticketExchange.setBuyer(buyer);
        ticketExchange = ticketExchangeRepository.save(ticketExchange);

        // Update order
        Order order = ticketExchange.getOrder();
        order.setUser(buyer);
        order.setStatus(OrderStatus.TRANSFERRED.name());
        orderRepository.save(order);

        // Send notifications
        try {
            notificationService.sendNotificationToUser(
                    seller.getId(),
                    "Vé ăn trưa đã được chuyển nhượng",
                    "Vé ăn trưa ngày " + order.getMenu().getMenuDate() + " của bạn đã được " + buyer.getFullName()
                            + " nhận thành công.");
            notificationService.sendNotificationToUser(
                    buyer.getId(),
                    "Nhận vé ăn trưa thành công",
                    "Bạn đã nhận thành công vé ăn trưa ngày " + order.getMenu().getMenuDate() + " từ "
                            + seller.getFullName() + ".");
        } catch (Exception e) {
            log.error("Failed to send notification for ticket exchange claim", e);
        }

        return ticketExchangeMapper.toDto(ticketExchange);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketExchangeResponse> getAdminExchanges(LocalDate startDate, String status) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<TicketExchange> list;
        if (status != null && !status.isEmpty()) {
            list = ticketExchangeRepository.findByCreatedAtAfterAndStatus(startInstant, status.toUpperCase());
        } else {
            list = ticketExchangeRepository.findByCreatedAtAfter(startInstant);
        }
        return ticketExchangeMapper.toDtoList(list);
    }

}
