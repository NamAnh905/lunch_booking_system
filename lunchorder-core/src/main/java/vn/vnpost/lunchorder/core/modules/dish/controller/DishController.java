package vn.vnpost.lunchorder.core.modules.dish.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.DishService;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishCreateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishUpdateRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import vn.vnpost.lunchorder.system.modules.excel.service.ExcelExportService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dishes")
public class DishController {
    private final DishService dishService;
    private final ExcelExportService excelExportService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_DISHES')")
    public ApiResponse<DishResponse> create(@RequestBody @Valid DishCreateRequest request) {
        return ApiResponse.<DishResponse>builder()
                .result(dishService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_DISHES')")
    public ApiResponse<DishResponse> update(@PathVariable Long id, @RequestBody @Valid DishUpdateRequest request) {
        return ApiResponse.<DishResponse>builder()
                .result(dishService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_DISHES')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_DISHES')")
    public ApiResponse<PageResponse<DishResponse>> findAll(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "types", required = false) List<String> types,
            @RequestParam(value = "isActives", required = false) List<Boolean> isActives) {
        return ApiResponse.<PageResponse<DishResponse>>builder()
                .result(dishService.findAll(page, size, keyword, types, isActives))
                .build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('VIEW_DISHES')")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            List<DishResponse> list = dishService.export(keyword);
            ByteArrayInputStream in = excelExportService.exportToExcel(list, "Danh sách món ăn");
            byte[] excelData = in.readAllBytes();
            String filename = "danh_sach_mon_an.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_DISHES')")
    public ApiResponse<List<DishResponse>> search(@RequestParam String name) {
        return ApiResponse.<List<DishResponse>>builder()
                .result(dishService.search(name))
                .build();
    }
}
