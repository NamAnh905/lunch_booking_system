package vn.vnpost.lunchorder.core.modules.notification.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.core.modules.notification.service.NotificationService;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationResponse;
import vn.vnpost.lunchorder.core.modules.notification.service.dto.NotificationSendRequest;
import vn.vnpost.lunchorder.system.security.jwt.UserPrincipal;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
    public ApiResponse<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size) {
        Page<NotificationResponse> result = notificationService.getMyNotifications(userPrincipal.getUserId(),
                PageRequest.of(page, PaginationConstants.clampSize(size)));
        return ApiResponse.<Page<NotificationResponse>>builder()
                .result(result)
                .build();
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
    public ApiResponse<String> markAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        notificationService.markAsRead(userPrincipal.getUserId(), id);
        return ApiResponse.<String>builder()
                .result("Notification marked as read")
                .build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('CREATE_OWN_ORDER')")
    public ApiResponse<String> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal.getUserId());
        return ApiResponse.<String>builder()
                .result("All notifications marked as read")
                .build();
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('SEND_NOTIFICATIONS')")
    public ApiResponse<String> sendNotification(
            @RequestBody @Valid NotificationSendRequest request) {
        notificationService.sendNotification(request);
        return ApiResponse.<String>builder()
                .result("Notification sent successfully")
                .build();
    }
}
