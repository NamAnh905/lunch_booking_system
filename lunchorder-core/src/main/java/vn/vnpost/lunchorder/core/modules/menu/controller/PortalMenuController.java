package vn.vnpost.lunchorder.core.modules.menu.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.MenuService;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/portal/menus")
@PreAuthorize("hasAuthority('VIEW_MENU')")
public class PortalMenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<PageResponse<MenuResponse>> getMenus(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.<PageResponse<MenuResponse>>builder()
                .result(menuService.findAll(page, size, keyword))
                .build();
    }

    @GetMapping("/by-date")
    public ApiResponse<List<MenuResponse>> getMenusByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.<List<MenuResponse>>builder()
                .result(menuService.findByDate(date))
                .build();
    }

    @GetMapping("/weekly")
    public ApiResponse<List<MenuResponse>> getWeeklyMenus(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.<List<MenuResponse>>builder()
                .result(menuService.findByDateRange(startDate, endDate))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MenuResponse> findById(@PathVariable Long id) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.findById(id))
                .build();
    }
}
