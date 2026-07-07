package vn.vnpost.lunchorder.core.modules.price.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceUpdateRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotNull(message = "Giá không được để trống")
    private BigDecimal amount;

    private String description;

    private Boolean isActive;
}
