package vn.vnpost.lunchorder.core.modules.notification.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Boolean isRead;
    private Instant createdAt;
}
