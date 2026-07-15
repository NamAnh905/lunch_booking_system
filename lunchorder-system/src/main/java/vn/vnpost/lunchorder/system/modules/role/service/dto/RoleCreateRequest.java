package vn.vnpost.lunchorder.system.modules.role.service.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;

@Getter
@Setter
public class RoleCreateRequest {
    @NotBlank(message = "Mã vai trò không được để trống.")
    @Size(max = 50, message = "Mã vai trò không được vượt quá 50 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_CODE, message = "Mã vai trò chỉ được chứa chữ in hoa và dấu gạch dưới.")
    private String code;

    @NotBlank(message = "Tên vai trò không được để trống.")
    @Size(max = 255, message = "Tên vai trò không được vượt quá 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_GENERAL_NAME, message = "Tên vai trò không được chứa ký tự đặc biệt.")
    private String name;

    private String description;

    private Set<String> permissions;
}
