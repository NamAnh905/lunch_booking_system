package vn.vnpost.lunchorder.core.modules.menu.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.ExcelDownload;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.MenuService;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuImageCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/menus")
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<MenuResponse> create(@RequestBody @Valid MenuCreateRequest request) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.create(request))
                .build();
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<MenuResponse> createImageMenu(
            @RequestPart("request") @Valid MenuImageCreateRequest request,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.createImageMenu(request, file))
                .build();
    }

    @PutMapping(value = "/image/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<MenuResponse> updateImageMenu(@PathVariable Long id,
            @RequestPart("request") @Valid MenuImageCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.updateImageMenu(id, request, file))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<MenuResponse> update(@PathVariable Long id, @RequestBody @Valid MenuUpdateRequest request) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<PageResponse<MenuResponse>> getMenus(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.<PageResponse<MenuResponse>>builder()
                .result(menuService.findAll(page, size, keyword))
                .build();
    }

    @GetMapping("/weekly")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<List<MenuResponse>> getWeeklyMenus(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.<List<MenuResponse>>builder()
                .result(menuService.findByDateRange(startDate, endDate))
                .build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ExcelDownload.of(menuService.exportExcel(keyword), "danh_sach_thuc_don.xlsx");
    }
}
