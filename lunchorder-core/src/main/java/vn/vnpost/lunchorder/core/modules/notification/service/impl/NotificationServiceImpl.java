package vn.vnpost.lunchorder.core.modules.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.notification.entity.Notification;
import vn.vnpost.lunchorder.core.modules.notification.event.NotificationCreatedEvent;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.notification.repository.NotificationRepository;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationSendRequest;
import vn.vnpost.lunchorder.core.modules.notification.service.mapstruct.NotificationMapper;
import vn.vnpost.lunchorder.system.modules.user.service.UserLookupService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserLookupService userLookupService;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notificationMapper.toDtoPage(page);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * Kích thước lô khi broadcast notification cho toàn bộ user. Mỗi lô được nạp từ DB,
     * tạo notification rồi lưu và giải phóng, tránh nạp toàn bộ user vào RAM (rủi ro OOM).
     */
    private static final int BROADCAST_BATCH_SIZE = 500;

    /**
     * Không đặt {@code @Transactional} ở cấp method: nhánh broadcast phải để mỗi lô commit
     * độc lập (xem {@link #broadcastToAllUsers}). Nhánh gửi cho 1 user chỉ có một lệnh
     * {@code save} vốn đã tự chạy trong transaction riêng của repository.
     */
    @Override
    public void sendNotification(NotificationSendRequest request) {
        if (request.getUserId() != null) {
            sendNotificationToUser(request.getUserId(), request.getTitle(), request.getContent());
        } else {
            broadcastToAllUsers(request);
        }
    }

    /**
     * Broadcast notification cho toàn bộ user theo lô. Duyệt user theo trang (size
     * {@link #BROADCAST_BATCH_SIZE}); mỗi trang tạo và lưu notifications rồi giải phóng, tránh
     * nạp toàn bộ user vào RAM (rủi ro OOM).
     * <p>
     * Không đặt {@code @Transactional} bao trọn vòng lặp để không giữ một transaction quá lớn:
     * mỗi lần {@code saveAll} tự chạy trong transaction riêng của repository (SimpleJpaRepository),
     * nên mỗi trang được commit độc lập.
     */
    private void broadcastToAllUsers(NotificationSendRequest request) {
        int pageNumber = 0;
        Page<User> page;
        do {
            Pageable pageable = PageRequest.of(pageNumber, BROADCAST_BATCH_SIZE);
            page = userLookupService.findAll(pageable);

            List<Notification> notifications = page.getContent().stream().map(user -> {
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setTitle(request.getTitle());
                notification.setContent(request.getContent());
                notification.setIsRead(false);
                return notification;
            }).collect(Collectors.toList());

            List<Notification> saved = notificationRepository.saveAll(notifications);
            saved.forEach(this::publishCreatedEvent);
            pageNumber++;
        } while (page.hasNext());
    }

    @Override
    @Transactional
    public NotificationResponse sendNotificationToUser(Long userId, String title, String content) {
        User user = userLookupService.getById(userId);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notification = notificationRepository.save(notification);

        return publishCreatedEvent(notification);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationResponse publishCreatedEvent(Notification notification) {
        NotificationResponse dto = notificationMapper.toDto(notification);
        eventPublisher.publishEvent(new NotificationCreatedEvent(dto.getUserId(), dto));
        return dto;
    }
}
