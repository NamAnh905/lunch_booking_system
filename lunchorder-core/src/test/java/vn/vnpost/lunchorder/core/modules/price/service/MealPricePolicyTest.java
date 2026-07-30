package vn.vnpost.lunchorder.core.modules.price.service;

import org.junit.jupiter.api.Test;
import vn.vnpost.lunchorder.common.enums.MealType;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MealPricePolicyTest {

    private MealPricePolicy policyWith(PriceResponse... activePrices) {
        PriceService priceService = mock(PriceService.class);
        when(priceService.getActivePrices()).thenReturn(List.of(activePrices));
        return new MealPricePolicy(priceService);
    }

    private PriceResponse price(MealType mealType, String amount) {
        PriceResponse response = new PriceResponse();
        response.setMealType(mealType);
        response.setAmount(amount == null ? null : new BigDecimal(amount));
        return response;
    }

    @Test
    void layGiaTheoLoaiSuatDangHoatDong() {
        MealPricePolicy policy = policyWith(
                price(MealType.NORMAL, "30000"),
                price(MealType.SPECIAL, "45000"));

        assertThat(policy.getNormalPrice()).isEqualByComparingTo("30000");
        assertThat(policy.getSpecialPrice()).isEqualByComparingTo("45000");
    }

    @Test
    void giaThuongCaoHonGiaDacBiet_thiVanTraDungTheoLoaiSuat() {
        MealPricePolicy policy = policyWith(
                price(MealType.NORMAL, "50000"),
                price(MealType.SPECIAL, "20000"));

        assertThat(policy.getNormalPrice()).isEqualByComparingTo("50000");
        assertThat(policy.getSpecialPrice()).isEqualByComparingTo("20000");
    }

    @Test
    void giaThuongVuotNguong25000_thiSuatDacBietKhongBiSuyNhamThanhSuatThuong() {
        MealPricePolicy policy = policyWith(
                price(MealType.NORMAL, "35000"),
                price(MealType.SPECIAL, "35000"));

        assertThat(policy.resolvePrice(MealType.NORMAL)).isEqualByComparingTo("35000");
        assertThat(policy.resolvePrice(MealType.SPECIAL)).isEqualByComparingTo("35000");
    }

    @Test
    void khongCoBangGiaHoatDong_thiDungGiaDuPhong() {
        MealPricePolicy policy = policyWith();

        assertThat(policy.getNormalPrice()).isEqualByComparingTo(MealPricePolicy.DEFAULT_NORMAL_PRICE);
        assertThat(policy.getSpecialPrice()).isEqualByComparingTo(MealPricePolicy.DEFAULT_SPECIAL_PRICE);
    }

    @Test
    void thieuLoaiSuatDacBiet_thiChiSuatDacBietDungGiaDuPhong() {
        MealPricePolicy policy = policyWith(price(MealType.NORMAL, "30000"));

        assertThat(policy.getNormalPrice()).isEqualByComparingTo("30000");
        assertThat(policy.getSpecialPrice()).isEqualByComparingTo(MealPricePolicy.DEFAULT_SPECIAL_PRICE);
    }

    @Test
    void giaTriTienNull_thiDungGiaDuPhong() {
        MealPricePolicy policy = policyWith(price(MealType.NORMAL, null));

        assertThat(policy.getNormalPrice()).isEqualByComparingTo(MealPricePolicy.DEFAULT_NORMAL_PRICE);
    }
}
