package vn.vnpost.lunchorder.system.modules.permission.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionCreateRequest {
    @NotBlank(message = "Hành động không được để trống.")
    private String action;

    @NotBlank(message = "Mô tả không được để trống.")
    private String description;
}
