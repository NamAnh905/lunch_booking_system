package vn.vnpost.lunchorder.system.modules.permission.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionUpdateRequest {
    @NotBlank(message = "Mô tả không được để trống.")
    private String description;
}
