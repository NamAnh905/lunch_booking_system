package vn.vnpost.lunchorder.system.modules.permission.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;

@Getter
@Setter
public class PermissionCreateRequest {
    @NotBlank(message = "Hành động không được để trống.")
    @Size(max = 100, message = "Mã quyền không được vượt quá 100 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_CODE, message = "Mã quyền chỉ được chứa chữ in hoa và dấu gạch dưới.")
    private String action;

    @NotBlank(message = "Mô tả không được để trống.")
    private String description;
}
