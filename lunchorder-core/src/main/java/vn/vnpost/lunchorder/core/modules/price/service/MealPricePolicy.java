package vn.vnpost.lunchorder.core.modules.price.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;

import java.math.BigDecimal;

/**
 * Single source of truth for meal prices used across ordering and reporting.
 *
 * <p>Prices are derived from the active {@code Price} records (the lowest active
 * price is treated as the "normal" meal, the highest as the "special" meal),
 * falling back to system defaults when no active price is configured. This
 * replaces the hard-coded 25.000/40.000 values that previously lived inside
 * {@code OrderServiceImpl}, so ordering and reporting can never disagree.</p>
 */
@Component
@RequiredArgsConstructor
public class MealPricePolicy {

    static final BigDecimal DEFAULT_NORMAL_PRICE = new BigDecimal("25000");
    static final BigDecimal DEFAULT_SPECIAL_PRICE = new BigDecimal("40000");

    private final PriceService priceService;

    public BigDecimal getNormalPrice() {
        return priceService.getActivePrices().stream()
                .map(PriceResponse::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(DEFAULT_NORMAL_PRICE);
    }

    public BigDecimal getSpecialPrice() {
        return priceService.getActivePrices().stream()
                .map(PriceResponse::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(DEFAULT_SPECIAL_PRICE);
    }

    public BigDecimal resolvePrice(boolean isSpecial) {
        return isSpecial ? getSpecialPrice() : getNormalPrice();
    }
}
