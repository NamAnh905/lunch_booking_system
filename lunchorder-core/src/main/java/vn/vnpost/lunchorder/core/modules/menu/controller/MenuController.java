package vn.vnpost.lunchorder.core.modules.menu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.MenuService;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import vn.vnpost.lunchorder.system.modules.excel.service.ExcelExportService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/menus")
public class MenuController {

    private final MenuService menuService;
    private final ExcelExportService excelExportService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<MenuResponse> create(@RequestBody @Valid MenuCreateRequest request) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.create(request))
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
    @PreAuthorize("hasAnyAuthority('VIEW_ADMIN_MENUS', 'CREATE_OWN_ORDER')")
    public ApiResponse<?> getMenus(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        if (date != null) {
            return ApiResponse.<List<MenuResponse>>builder()
                    .result(menuService.findByDate(date))
                    .build();
        }
        return ApiResponse.<PageResponse<MenuResponse>>builder()
                .result(menuService.findAll(page, size, keyword))
                .build();
    }

    @GetMapping("/weekly")
    @PreAuthorize("hasAnyAuthority('VIEW_ADMIN_MENUS', 'CREATE_OWN_ORDER')")
    public ApiResponse<List<MenuResponse>> getWeeklyMenus(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.<List<MenuResponse>>builder()
                .result(menuService.findByDateRange(startDate, endDate))
                .build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('VIEW_ADMIN_MENUS', 'CREATE_OWN_ORDER')")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            List<MenuResponse> list = menuService.export(keyword);
            ByteArrayInputStream in = excelExportService.exportToExcel(list, "Danh sách thực đơn");
            byte[] excelData = in.readAllBytes();
            String filename = "danh_sach_thuc_don.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_ADMIN_MENUS', 'CREATE_OWN_ORDER')")
    public ApiResponse<MenuResponse> findById(@PathVariable Long id) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.findById(id))
                .build();
    }
}
