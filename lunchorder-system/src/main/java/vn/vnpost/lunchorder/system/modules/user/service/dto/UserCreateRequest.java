package vn.vnpost.lunchorder.system.modules.user.service.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.constant.ValidationConstants;

@Getter
@Setter
public class UserCreateRequest {
    @NotBlank(message = "Username không được để trống.")
    @Size(min = 10, max = 50, message = "Username phải có độ dài từ 10 đến 50 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_ACCOUNT, message = "Username chỉ được chứa chữ số, không có khoảng trắng.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 8, max = 255, message = "Mật khẩu phải có độ dài từ 8 đến 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_PASSWORD, message = "Mật khẩu không được chứa khoảng trắng.")
    private String password;

    @NotBlank(message = "Họ tên không được để trống.")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự.")
    @Pattern(regexp = ValidationConstants.REGEX_PERSON_NAME, message = "Họ tên chỉ được chứa chữ cái và khoảng trắng.")
    private String fullName;

    @NotBlank(message = "Phòng/Ban không được để trống.")
    private String department;

    private Set<String> roles;
}
