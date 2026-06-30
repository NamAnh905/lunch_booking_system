package vn.vnpost.lunchorder.system.modules.user.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" })
public class UserResponse extends BaseResponse {
    private String username;
    private String employeeCode;
    private String fullName;
    private String email;
    private String department;
    private Boolean isActive;
}
