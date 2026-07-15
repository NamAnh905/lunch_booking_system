package vn.vnpost.lunchorder.system.modules.user.service.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;

@Getter
@Setter
public class UserUpdateRequest {
    @NotBlank(message = "Họ tên không được để trống.")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_PERSON_NAME, message = "Họ tên chỉ được chứa chữ cái và khoảng trắng.")
    private String fullName;

    @NotBlank(message = "Phòng/Ban không được để trống.")
    private String department;

    @NotNull(message = "Trạng thái hoạt động không được để trống.")
    private Boolean isActive;

    @Size(min = 8, max = 255, message = "Mật khẩu phải có độ dài từ 8 đến 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_PASSWORD, message = "Mật khẩu không được chứa khoảng trắng.")
    private String password;

    private Set<String> roles;
}
