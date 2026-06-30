package vn.vnpost.lunchorder.core.modules.dish.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DishCreateRequest {
    @NotBlank(message = "Tên món ăn không được để trống.")
    private String name;

    private String description;

    private Boolean isActive = true;
}
