package vn.vnpost.lunchorder.system.modules.department.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;

@Getter
@Setter
public class DepartmentUpdateRequest {
    @NotBlank(message = "Tên phòng ban không được để trống.")
    @Size(max = 255, message = "Tên phòng ban không được vượt quá 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_GENERAL_NAME, message = "Tên phòng ban không được chứa ký tự đặc biệt.")
    private String name;
}
