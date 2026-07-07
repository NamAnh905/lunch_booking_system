package vn.vnpost.lunchorder.core.modules.feedback.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.entity.Feedback;
import vn.vnpost.lunchorder.common.entity.Menu;
import vn.vnpost.lunchorder.common.entity.Order;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.enums.OrderStatus;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.feedback.repository.FeedbackRepository;
import vn.vnpost.lunchorder.core.modules.feedback.service.FeedbackService;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackCreateRequest;
import vn.vnpost.lunchorder.core.modules.feedback.service.dto.FeedbackResponse;
import vn.vnpost.lunchorder.core.modules.feedback.service.mapstruct.FeedbackMapper;
import vn.vnpost.lunchorder.core.modules.menu.repository.MenuRepository;
import vn.vnpost.lunchorder.core.modules.order.repository.OrderRepository;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final FeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(Long userId, FeedbackCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Menu menu = menuRepository.findById(request.getMenuId())
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));

        // Check if user has ordered this menu (and order is not cancelled)
        Optional<Order> orderOpt = orderRepository.findByUserIdAndMenuId(userId, request.getMenuId());
        if (orderOpt.isEmpty() || OrderStatus.CANCELLED.name().equalsIgnoreCase(orderOpt.get().getStatus())) {
            throw new AppException(ErrorCode.FEEDBACK_CANNOT_CREATE);
        }

        // Check if feedback already exists for this user and menu
        if (feedbackRepository.existsByUserIdAndMenuId(userId, request.getMenuId())) {
            throw new AppException(ErrorCode.FEEDBACK_ALREADY_EXISTS);
        }

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setMenu(menu);
        feedback.setComment(request.getComment());

        feedback = feedbackRepository.save(feedback);
        return feedbackMapper.toDto(feedback);
    }

    @Override
    public PageResponse<FeedbackResponse> getFeedbacks(int page) {
        int pageSize = 10;
        int pageNumber = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Feedback> feedbackPage = feedbackRepository.findAll(pageable);
        List<FeedbackResponse> dtoList = feedbackMapper.toDtoList(feedbackPage.getContent());

        return PageResponse.<FeedbackResponse>builder()
                .currentPage(page)
                .totalPages(feedbackPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(feedbackPage.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));
        feedbackRepository.delete(feedback);
    }
}
