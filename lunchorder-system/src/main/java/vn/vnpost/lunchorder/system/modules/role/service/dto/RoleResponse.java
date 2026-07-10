package vn.vnpost.lunchorder.system.modules.role.service.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionResponse;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" }, ignoreUnknown = true)
public class RoleResponse extends BaseResponse {
    private String code;
    private String name;
    private String description;
    private Set<PermissionResponse> permissions;
}
