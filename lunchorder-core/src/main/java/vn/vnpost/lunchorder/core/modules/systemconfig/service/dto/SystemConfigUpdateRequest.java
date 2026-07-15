package vn.vnpost.lunchorder.core.modules.systemconfig.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigUpdateRequest {
    @NotBlank(message = "Khóa cấu hình không được để trống.")
    private String configKey;

    @NotBlank(message = "Giá trị cấu hình không được để trống.")
    private String configValue;
}
