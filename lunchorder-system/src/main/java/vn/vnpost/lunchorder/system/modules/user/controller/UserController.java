package vn.vnpost.lunchorder.system.modules.user.controller;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.user.service.UserService;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserAssignRolesRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ApiResponse<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_USERS')")
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(value = "page", defaultValue = "1") int page) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.findAll(page))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_USERS')")
    public ApiResponse<List<UserResponse>> search(@RequestParam String keyword) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.search(keyword))
                .build();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ASSIGN_USER_ROLES')")
    public ApiResponse<Void> assignRoles(
            @PathVariable Long id,
            @RequestBody @Valid UserAssignRolesRequest request) {
        userService.assignRoles(id, request.getRoleCodes());
        return ApiResponse.<Void>builder()
                .message("Assign roles to user success")
                .build();
    }
}
