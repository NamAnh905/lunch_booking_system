package vn.vnpost.lunchorder.system.modules.user.service.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Phòng/Ban không được để trống.")
    private String department;

    @NotNull(message = "Trạng thái hoạt động không được để trống.")
    private Boolean isActive;

    private String password;

    private Set<String> roles;
}
