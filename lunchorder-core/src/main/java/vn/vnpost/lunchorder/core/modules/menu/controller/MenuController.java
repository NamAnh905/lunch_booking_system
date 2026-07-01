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

@RestController
@RequiredArgsConstructor
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
            @RequestParam(value = "page", defaultValue = "1") int page) {
        if (date != null) {
            return ApiResponse.<List<MenuResponse>>builder()
                    .result(menuService.findByDate(date))
                    .build();
        }
        return ApiResponse.<PageResponse<MenuResponse>>builder()
                .result(menuService.findAll(page))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_ADMIN_MENUS', 'CREATE_OWN_ORDER')")
    public ApiResponse<MenuResponse> findById(@PathVariable Long id) {
        return ApiResponse.<MenuResponse>builder()
                .result(menuService.findById(id))
                .build();
    }
}
