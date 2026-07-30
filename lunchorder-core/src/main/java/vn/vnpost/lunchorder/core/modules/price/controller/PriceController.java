package vn.vnpost.lunchorder.core.modules.price.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.price.service.PriceService;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceCreateRequest;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/admin/prices")
@RequiredArgsConstructor
@Validated
public class PriceController {

    private final PriceService priceService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ApiResponse<PriceResponse> createPrice(@RequestBody @Valid PriceCreateRequest request) {
        return ApiResponse.<PriceResponse>builder()
                .result(priceService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ApiResponse<PriceResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid PriceUpdateRequest request) {
        return ApiResponse.<PriceResponse>builder()
                .result(priceService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        priceService.delete(id);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ApiResponse<PageResponse<PriceResponse>> findAll(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.<PageResponse<PriceResponse>>builder()
                .result(priceService.findAll(page, size, keyword))
                .build();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ApiResponse<List<PriceResponse>> getActivePrices() {
        return ApiResponse.<List<PriceResponse>>builder()
                .result(priceService.getActivePrices())
                .build();
    }
}
