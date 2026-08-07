package vn.vnpost.lunchorder.system.modules.user.controller;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.ExcelDownload;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.user.service.UserService;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserImportResultResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserAssignRolesRequest;

@RestController
@RequiredArgsConstructor
@Validated
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
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "departmentIds", required = false) List<Long> departmentIds,
            @RequestParam(value = "isActives", required = false) List<Boolean> isActives,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDir", required = false) String sortDir) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.findAll(page, size, keyword, departmentIds, isActives, sortBy, sortDir))
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
        return ExcelDownload.of(userService.exportExcel(keyword), "danh_sach_nguoi_dung.xlsx");
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        return ExcelDownload.of(userService.buildImportTemplate(), "mau_nhap_nguoi_dung.xlsx");
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ApiResponse<UserImportResultResponse> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<UserImportResultResponse>builder()
                .result(userService.importExcel(file))
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
