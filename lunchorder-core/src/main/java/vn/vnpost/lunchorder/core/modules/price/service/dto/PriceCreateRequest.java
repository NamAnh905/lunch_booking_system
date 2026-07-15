package vn.vnpost.lunchorder.core.modules.price.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceCreateRequest {
    @NotBlank(message = "Tên không được để trống.")
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_GENERAL_NAME, message = "Tên không được chứa ký tự đặc biệt.")
    private String name;

    @NotNull(message = "Giá không được để trống.")
    @Min(value = 0, message = "Giá không được nhỏ hơn 0.")
    private BigDecimal amount;

    private String description;

    private Boolean isActive = true;
}
