package vn.vnpost.lunchorder.core.modules.dish.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;
import vn.vnpost.lunchorder.common.enums.DishType;

@Getter
@Setter
public class DishCreateRequest {
    @NotBlank(message = "Tên món ăn không được để trống.")
    @Size(max = 255, message = "Tên món ăn không được vượt quá 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_GENERAL_NAME, message = "Tên món ăn không được chứa ký tự đặc biệt.")
    private String name;

    private String description;

    private Boolean isActive = true;

    @NotNull(message = "Loại món ăn không được để trống.")
    private DishType type;
}
