package vn.vnpost.lunchorder.system.modules.permission.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" }, ignoreUnknown = true)
public class PermissionResponse extends BaseResponse {
    private String action;
    private String description;
}
