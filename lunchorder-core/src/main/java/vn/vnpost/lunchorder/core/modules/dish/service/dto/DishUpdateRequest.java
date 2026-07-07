package vn.vnpost.lunchorder.core.modules.dish.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.enums.DishType;

@Getter
@Setter
public class DishUpdateRequest {
    @NotBlank(message = "Tên món ăn không được để trống.")
    private String name;

    private String description;

    private Boolean isActive;

    @NotNull(message = "Loại món ăn không được để trống.")
    private DishType type;
}
