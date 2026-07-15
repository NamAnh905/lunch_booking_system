package vn.vnpost.lunchorder.system.modules.department.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.department.service.DepartmentService;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentCreateRequest;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentResponse;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentUpdateRequest;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_DEPARTMENTS')")
    public ApiResponse<DepartmentResponse> create(@RequestBody @Valid DepartmentCreateRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_DEPARTMENTS')")
    public ApiResponse<DepartmentResponse> update(@PathVariable Long id,
            @RequestBody @Valid DepartmentUpdateRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_DEPARTMENTS')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_DEPARTMENTS')")
    public ApiResponse<PageResponse<DepartmentResponse>> findAll(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size) {
        return ApiResponse.<PageResponse<DepartmentResponse>>builder()
                .result(departmentService.findAll(keyword, page, size))
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('VIEW_DEPARTMENTS')")
    public ApiResponse<List<DepartmentResponse>> getAll() {
        return ApiResponse.<List<DepartmentResponse>>builder()
                .result(departmentService.getAll())
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_DEPARTMENTS')")
    public ApiResponse<List<DepartmentResponse>> search(@RequestParam String keyword) {
        return ApiResponse.<List<DepartmentResponse>>builder()
                .result(departmentService.search(keyword))
                .build();
    }
}
