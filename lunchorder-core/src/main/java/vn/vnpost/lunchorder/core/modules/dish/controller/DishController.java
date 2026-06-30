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

@RestController
@RequiredArgsConstructor
@RequestMapping("/dishes")
public class DishController {
    private final DishService dishService;

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
            @RequestParam(value = "page", defaultValue = "1") int page) {
        return ApiResponse.<PageResponse<DishResponse>>builder()
                .result(dishService.findAll(page))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_DISHES')")
    public ApiResponse<List<DishResponse>> search(@RequestParam String name) {
        return ApiResponse.<List<DishResponse>>builder()
                .result(dishService.search(name))
                .build();
    }
}
