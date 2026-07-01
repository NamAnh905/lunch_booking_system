package vn.vnpost.lunchorder.system.modules.role.service.dto;

import java.util.Set;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleAssignPermissionsRequest {
    @NotEmpty(message = "Danh sách quyền không được để trống.")
    private Set<String> permissionCodes;
}
