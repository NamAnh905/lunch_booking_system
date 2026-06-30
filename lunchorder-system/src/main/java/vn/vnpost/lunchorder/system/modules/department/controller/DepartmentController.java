package vn.vnpost.lunchorder.system.modules.department.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.department.service.DepartmentService;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentCreateRequest;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentResponse;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentUpdateRequest;

@RestController
@RequiredArgsConstructor
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
            @RequestParam(value = "page", defaultValue = "1") int page) {
        return ApiResponse.<PageResponse<DepartmentResponse>>builder()
                .result(departmentService.findAll(page))
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
