package vn.vnpost.lunchorder.system.modules.user.service.dto;

import java.util.Set;

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

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Phòng/Ban không được để trống.")
    private String department;

    private Set<String> roles;
}
