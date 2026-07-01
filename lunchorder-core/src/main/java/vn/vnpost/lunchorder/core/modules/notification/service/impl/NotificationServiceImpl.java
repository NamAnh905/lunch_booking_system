package vn.vnpost.lunchorder.core.modules.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.entity.Notification;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.notification.repository.NotificationRepository;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationSendRequest;
import vn.vnpost.lunchorder.core.modules.notification.service.mapstruct.NotificationMapper;
import vn.vnpost.lunchorder.system.modules.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public void sendNotification(NotificationSendRequest request) {
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(request.getTitle());
            notification.setContent(request.getContent());
            notification.setIsRead(false);
            notificationRepository.save(notification);
        } else {
            // Broadcast to all users
            List<User> allUsers = userRepository.findAll();
            List<Notification> notifications = allUsers.stream().map(user -> {
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setTitle(request.getTitle());
                notification.setContent(request.getContent());
                notification.setIsRead(false);
                return notification;
            }).collect(Collectors.toList());
            notificationRepository.saveAll(notifications);
        }
    }

    @Override
    @Transactional
    public void sendNotificationToUser(Long userId, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }
}
