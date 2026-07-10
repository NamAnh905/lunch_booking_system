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
@Transactional(readOnly = true)
public class TicketExchangeServiceImpl implements TicketExchangeService {

    private final TicketExchangeRepository ticketExchangeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final TicketExchangeMapper ticketExchangeMapper;
    private final NotificationService notificationService;

    private boolean isValidExchangeTimeWindow(LocalDate menuDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime cutOffTime = getCutOffTime();
        LocalDateTime cutOffStart = menuDate.minusDays(1).atTime(cutOffTime);
        LocalDateTime cutOffEnd = menuDate.atTime(10, 30);
        return !now.isBefore(cutOffStart) && !now.isAfter(cutOffEnd);
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
    public PageResponse<TicketExchangeResponse> getOpenExchanges(int page, int size, String status, String keyword) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<TicketExchange> entityPage = ticketExchangeRepository.findForAdmin(null, null, status, keyword, pageable);
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
    public List<TicketExchangeResponse> getMyListedTickets(Long userId) {
        List<TicketExchange> entityList = ticketExchangeRepository.findBySellerIdAndStatus(userId, TicketExchangeStatus.OPEN.name());
        return entityList.stream()
                .map(ticketExchangeMapper::toDto)
                .toList();
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
        LocalDate menuDate = order.getOrderDate();
        if (!isValidExchangeTimeWindow(menuDate)) {
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
        LocalDate menuDate = ticketExchange.getOrder().getOrderDate();
        LocalDate today = LocalDate.now();
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        ticketExchange.setStatus(TicketExchangeStatus.CANCELLED.name());
        ticketExchangeRepository.save(ticketExchange);
    }

    @Override
    @Transactional
    public void forceCancelTicket(Long exchangeId) {
        TicketExchange ticketExchange = ticketExchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (!TicketExchangeStatus.OPEN.name().equalsIgnoreCase(ticketExchange.getStatus())) {
            throw new AppException(ErrorCode.EXCHANGE_NOT_OPEN);
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
        LocalDate menuDate = ticketExchange.getOrder().getOrderDate();
        boolean hasActiveOrderOnSameDay = orderRepository.findByUserIdAndOrderDateBetween(userId, menuDate, menuDate).stream()
                .anyMatch(o -> !OrderStatus.CANCELLED.name().equalsIgnoreCase(o.getStatus()));
        if (hasActiveOrderOnSameDay) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        // Check time constraints: Must be within exchange time window
        if (!isValidExchangeTimeWindow(menuDate)) {
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
        order.setStatus(OrderStatus.PENDING.name());
        orderRepository.save(order);

        // Send notifications
        try {
            notificationService.sendNotificationToUser(
                    seller.getId(),
                    "Vé ăn trưa đã được chuyển nhượng",
                    "Vé ăn trưa ngày " + order.getOrderDate() + " của bạn đã được " + buyer.getFullName()
                            + " nhận thành công.");
            notificationService.sendNotificationToUser(
                    buyer.getId(),
                    "Nhận vé ăn trưa thành công",
                    "Bạn đã nhận thành công vé ăn trưa ngày " + order.getOrderDate() + " từ "
                            + seller.getFullName() + ".");
        } catch (Exception e) {
            log.error("Failed to send notification for ticket exchange claim", e);
        }

        return ticketExchangeMapper.toDto(ticketExchange);
    }

    @Override
    public PageResponse<TicketExchangeResponse> getAdminExchanges(int page, int size, LocalDate startDate, LocalDate endDate, String status, String keyword) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, size);
        
        Page<TicketExchange> entityPage = ticketExchangeRepository.findForAdmin(startDate, endDate, status, keyword, pageable);
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

}
