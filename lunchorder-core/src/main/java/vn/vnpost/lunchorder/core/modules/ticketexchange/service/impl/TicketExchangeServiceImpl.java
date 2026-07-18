package vn.vnpost.lunchorder.core.modules.ticketexchange.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.core.modules.order.entity.Order;
import vn.vnpost.lunchorder.core.modules.ticketexchange.entity.TicketExchange;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.core.policy.CutOffPolicy;
import vn.vnpost.lunchorder.core.modules.ticketexchange.repository.TicketExchangeRepository;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.TicketExchangeService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeCreateRequest;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.mapstruct.TicketExchangeMapper;
import vn.vnpost.lunchorder.system.modules.user.service.UserLookupService;

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
    private final UserLookupService userLookupService;
    private final TicketExchangeMapper ticketExchangeMapper;
    private final NotificationService notificationService;
    private final CutOffPolicy cutOffPolicy;

    @Override
    public PageResponse<TicketExchangeResponse> getOpenExchanges(Long currentUserId, int page, int size, String keyword) {
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, PaginationConstants.clampSize(size));

        Page<TicketExchange> entityPage = ticketExchangeRepository.findOpenForMarket(
                TicketExchangeStatus.OPEN, currentUserId, keyword, pageable);
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
        List<TicketExchange> entityList = ticketExchangeRepository.findBySellerIdAndStatus(userId, TicketExchangeStatus.OPEN);
        return entityList.stream()
                .map(ticketExchangeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TicketExchangeResponse postTicketToMarket(Long userId, TicketExchangeCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_CANNOT_PASS);
        }

        Optional<TicketExchange> existingExchangeOpt = ticketExchangeRepository.findByOrderId(order.getId());
        if (existingExchangeOpt.isPresent() && existingExchangeOpt.get().getStatus() == TicketExchangeStatus.OPEN) {
            throw new AppException(ErrorCode.ORDER_IN_MARKET);
        }

        if (existingExchangeOpt.isPresent() && isClaimedBy(existingExchangeOpt.get(), userId)) {
            throw new AppException(ErrorCode.ORDER_CLAIMED_CANNOT_PASS);
        }

        LocalDate menuDate = order.getOrderDate();
        if (!cutOffPolicy.isWithinExchangeWindow(menuDate)) {
            throw new AppException(ErrorCode.ORDER_CANNOT_PASS);
        }

        TicketExchange ticketExchange;
        if (existingExchangeOpt.isPresent()) {
            ticketExchange = existingExchangeOpt.get();
            ticketExchange.setStatus(TicketExchangeStatus.OPEN);
            ticketExchange.setBuyer(null);
        } else {
            ticketExchange = new TicketExchange();
            ticketExchange.setOrder(order);
            ticketExchange.setStatus(TicketExchangeStatus.OPEN);
        }

        try {
            ticketExchange = ticketExchangeRepository.save(ticketExchange);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.ORDER_IN_MARKET);
        }
        return ticketExchangeMapper.toDto(ticketExchange);
    }

    @Override
    @Transactional
    public void withdrawTicketFromMarket(Long userId, Long exchangeId) {
        TicketExchange ticketExchange = ticketExchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (ticketExchange.getStatus() != TicketExchangeStatus.OPEN) {
            throw new AppException(ErrorCode.EXCHANGE_NOT_OPEN);
        }

        if (!ticketExchange.getOrder().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        LocalDate menuDate = ticketExchange.getOrder().getOrderDate();
        LocalDate today = LocalDate.now();
        if (menuDate.isBefore(today)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        ticketExchange.setStatus(TicketExchangeStatus.CANCELLED);
        ticketExchangeRepository.save(ticketExchange);
    }

    @Override
    @Transactional
    public TicketExchangeResponse claimTicket(Long userId, Long exchangeId) {
        TicketExchange ticketExchange = ticketExchangeRepository.findByIdForUpdate(exchangeId)
                .orElseThrow(() -> new AppException(ErrorCode.EXCHANGE_NOT_FOUND));

        if (ticketExchange.getStatus() != TicketExchangeStatus.OPEN) {
            throw new AppException(ErrorCode.EXCHANGE_NOT_OPEN);
        }

        if (ticketExchange.getOrder().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_CLAIM_OWN_TICKET);
        }

        LocalDate menuDate = ticketExchange.getOrder().getOrderDate();
        boolean hasActiveOrderOnSameDay = orderRepository.findByUserIdAndOrderDateBetween(userId, menuDate, menuDate, TicketExchangeStatus.OPEN).stream()
                .anyMatch(o -> o.getStatus() != OrderStatus.CANCELLED);
        if (hasActiveOrderOnSameDay) {
            throw new AppException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        if (!cutOffPolicy.isWithinExchangeWindow(menuDate)) {
            throw new AppException(ErrorCode.ORDER_CUTOFF_REACHED);
        }

        User buyer = userLookupService.getById(userId);

        User seller = orderRepository.findById(ticketExchange.getOrder().getId())
                .map(Order::getUser)
                .orElse(ticketExchange.getOrder().getUser());

        ticketExchange.setStatus(TicketExchangeStatus.MATCHED);
        ticketExchange.setBuyer(buyer);
        ticketExchange = ticketExchangeRepository.save(ticketExchange);

        Order order = ticketExchange.getOrder();
        order.setUser(buyer);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

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
        Pageable pageable = PageRequest.of(pageNumber, PaginationConstants.clampSize(size));
        
        Page<TicketExchange> entityPage = ticketExchangeRepository.findForAdmin(startDate, endDate, parseStatusOrNull(status), keyword, pageable);
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

    private boolean isClaimedBy(TicketExchange ticketExchange, Long userId) {
        return ticketExchange.getStatus() == TicketExchangeStatus.MATCHED
                && ticketExchange.getBuyer() != null
                && ticketExchange.getBuyer().getId().equals(userId);
    }

    private TicketExchangeStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TicketExchangeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ENUM_VALUE);
        }
    }

}
