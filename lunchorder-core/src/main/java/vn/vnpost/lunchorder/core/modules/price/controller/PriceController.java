package vn.vnpost.lunchorder.core.modules.price.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.core.modules.price.service.PriceService;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceCreateRequest;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/admin/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ResponseEntity<PriceResponse> create(@RequestBody @Valid PriceCreateRequest request) {
        return ResponseEntity.ok(priceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ResponseEntity<PriceResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid PriceUpdateRequest request) {
        return ResponseEntity.ok(priceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        priceService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ResponseEntity<PageResponse<PriceResponse>> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(priceService.findAll(page, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PRICE')")
    public ResponseEntity<PriceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(priceService.findById(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PriceResponse>> getActivePrices() {
        return ResponseEntity.ok(priceService.getActivePrices());
    }
}
