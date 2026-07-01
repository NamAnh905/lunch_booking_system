package vn.vnpost.lunchorder.system.modules.user.service.dto;

import java.util.Set;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAssignRolesRequest {
    @NotEmpty(message = "Danh sách vai trò không được để trống.")
    private Set<String> roleCodes;
}
