package vn.vnpost.lunchorder.system.modules.role.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.role.service.RoleService;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleCreateRequest;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleResponse;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleUpdateRequest;

import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleAssignPermissionsRequest;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/roles")
public class RoleController {
    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ApiResponse<RoleResponse> create(@RequestBody @Valid RoleCreateRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @RequestBody @Valid RoleUpdateRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ROLES')")
    public ApiResponse<PageResponse<RoleResponse>> findAll(
            @RequestParam(value = "page", defaultValue = "1") int page) {
        return ApiResponse.<PageResponse<RoleResponse>>builder()
                .result(roleService.findAll(page))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_ROLES')")
    public ApiResponse<RoleResponse> findByCode(@RequestParam String code) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.findByCode(code))
                .build();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ASSIGN_PERMISSIONS')")
    public ApiResponse<Void> assignPermissions(
            @PathVariable Long id,
            @RequestBody @Valid RoleAssignPermissionsRequest request) {
        roleService.assignPermissions(id, request.getPermissionCodes());
        return ApiResponse.<Void>builder()
                .message("Assign permissions to role success")
                .build();
    }
}
