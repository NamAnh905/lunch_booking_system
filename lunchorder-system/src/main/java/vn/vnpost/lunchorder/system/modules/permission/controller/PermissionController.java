package vn.vnpost.lunchorder.system.modules.permission.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.PermissionService;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionCreateRequest;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionUpdateRequest;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    public ApiResponse<PermissionResponse> create(@RequestBody @Valid PermissionCreateRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    public ApiResponse<PermissionResponse> update(@PathVariable Long id,
            @RequestBody @Valid PermissionUpdateRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PERMISSIONS')")
    public ApiResponse<PageResponse<PermissionResponse>> findAll(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size) {
        return ApiResponse.<PageResponse<PermissionResponse>>builder()
                .result(permissionService.findAll(keyword, page, size))
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('VIEW_PERMISSIONS')")
    public ApiResponse<List<PermissionResponse>> getAll() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAll())
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_PERMISSIONS')")
    public ApiResponse<PermissionResponse> findByAction(@RequestParam String action) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.findByAction(action))
                .build();
    }
}
