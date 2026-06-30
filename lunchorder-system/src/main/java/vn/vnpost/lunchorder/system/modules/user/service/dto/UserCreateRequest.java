package vn.vnpost.lunchorder.system.modules.user.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {
    @NotBlank(message = "Username không được để trống.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    private String password;

    @NotBlank(message = "Mã nhân viên không được để trống.")
    private String employeeCode;

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @Email(message = "Email không đúng định dạng.")
    @NotBlank(message = "Email không được để trống.")
    private String email;

    @NotBlank(message = "Phòng/Ban không được để trống.")
    private String department;
}
