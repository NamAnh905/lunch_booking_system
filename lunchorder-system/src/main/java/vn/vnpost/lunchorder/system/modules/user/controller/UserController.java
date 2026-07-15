package vn.vnpost.lunchorder.system.modules.user.controller;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.tools.excel.ExcelExportService;
import vn.vnpost.lunchorder.system.modules.user.service.UserService;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserAssignRolesRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;
    private final ExcelExportService excelExportService;

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
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "departmentIds", required = false) List<Long> departmentIds,
            @RequestParam(value = "isActives", required = false) List<Boolean> isActives) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.findAll(page, size, keyword, departmentIds, isActives))
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('VIEW_USERS')")
    public ApiResponse<List<UserResponse>> getAll() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAll())
                .build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('VIEW_USERS')")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            List<UserResponse> list = userService.export(keyword);
            ByteArrayInputStream in = excelExportService.exportToExcel(list, "Danh sách người dùng");
            byte[] excelData = in.readAllBytes();
            String filename = "danh_sach_nguoi_dung.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
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
