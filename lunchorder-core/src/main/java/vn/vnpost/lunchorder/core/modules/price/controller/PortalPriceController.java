package vn.vnpost.lunchorder.core.modules.price.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.price.service.PriceService;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/portal/prices")
@PreAuthorize("hasAuthority('VIEW_PRICE')")
public class PortalPriceController {

    private final PriceService priceService;

    @GetMapping("/active")
    public ApiResponse<List<PriceResponse>> getActivePrices() {
        return ApiResponse.<List<PriceResponse>>builder()
                .result(priceService.getActivePrices())
                .build();
    }
}
