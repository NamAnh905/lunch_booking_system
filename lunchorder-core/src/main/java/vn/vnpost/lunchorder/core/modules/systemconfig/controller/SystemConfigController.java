package vn.vnpost.lunchorder.core.modules.systemconfig.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.SystemConfigService;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigResponse;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigUpdateRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_SYSTEM_CONFIG')")
    public ApiResponse<List<SystemConfigResponse>> findAll() {
        return ApiResponse.<List<SystemConfigResponse>>builder()
                .result(systemConfigService.findAll())
                .build();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('MANAGE_SYSTEM_CONFIG')")
    public ApiResponse<List<SystemConfigResponse>> updateAll(
            @RequestBody @Valid List<@Valid SystemConfigUpdateRequest> requests) {
        return ApiResponse.<List<SystemConfigResponse>>builder()
                .result(systemConfigService.updateAll(requests))
                .build();
    }
}
