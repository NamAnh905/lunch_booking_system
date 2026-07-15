package vn.vnpost.lunchorder.core.modules.systemconfig.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.SystemConfigService;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.BusinessConfigResponse;

/**
 * Read-only cutoff/lock times for any authenticated user (portal + admin),
 * separate from {@link SystemConfigController} which exposes/edits the full
 * config table and is admin-only. Lets the frontend stop hardcoding
 * cutoff/lock times in messages and calendar logic.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/business-config")
public class BusinessConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BusinessConfigResponse> getBusinessConfig() {
        return ApiResponse.<BusinessConfigResponse>builder()
                .result(systemConfigService.getBusinessTimes())
                .build();
    }
}
