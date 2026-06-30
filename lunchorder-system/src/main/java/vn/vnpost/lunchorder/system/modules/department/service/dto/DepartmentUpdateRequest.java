package vn.vnpost.lunchorder.system.modules.department.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentUpdateRequest {
    @NotBlank(message = "Tên phòng ban không được để trống.")
    private String name;
}
