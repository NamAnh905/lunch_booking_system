package vn.vnpost.lunchorder.system.modules.department.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentCreateRequest {
    @NotBlank(message = "Mã phòng ban không được để trống.")
    private String code;

    @NotBlank(message = "Tên phòng ban không được để trống.")
    private String name;
}
