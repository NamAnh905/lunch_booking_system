package vn.vnpost.lunchorder.system.modules.role.service.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCreateRequest {
    @NotBlank(message = "Mã vai trò không được để trống.")
    private String code;

    @NotBlank(message = "Tên vai trò không được để trống.")
    private String name;

    private Set<String> permissions;
}
