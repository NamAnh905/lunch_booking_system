package vn.vnpost.lunchorder.core.modules.price.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.vnpost.lunchorder.common.enums.MealType;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MealPricePolicy {

    static final BigDecimal DEFAULT_NORMAL_PRICE = new BigDecimal("25000");
    static final BigDecimal DEFAULT_SPECIAL_PRICE = new BigDecimal("40000");

    private final PriceService priceService;

    public BigDecimal getNormalPrice() {
        return resolvePrice(MealType.NORMAL);
    }

    public BigDecimal getSpecialPrice() {
        return resolvePrice(MealType.SPECIAL);
    }

    public BigDecimal resolvePrice(MealType mealType) {
        return priceService.getActivePrices().stream()
                .filter(price -> price.getMealType() == mealType)
                .map(PriceResponse::getAmount)
                .filter(amount -> amount != null)
                .findFirst()
                .orElseGet(() -> defaultPrice(mealType));
    }

    private BigDecimal defaultPrice(MealType mealType) {
        return mealType == MealType.SPECIAL ? DEFAULT_SPECIAL_PRICE : DEFAULT_NORMAL_PRICE;
    }
}
