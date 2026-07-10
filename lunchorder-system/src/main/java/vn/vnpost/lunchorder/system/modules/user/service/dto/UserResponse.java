package vn.vnpost.lunchorder.system.modules.user.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.system.modules.excel.annotation.ExcelColumn;

import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" }, ignoreUnknown = true)
public class UserResponse extends BaseResponse {
    
    @Override
    @ExcelColumn(name = "ID", width = 3000)
    public Long getId() {
        return super.getId();
    }

    @ExcelColumn(name = "Tên đăng nhập", width = 5000)
    private String username;

    @ExcelColumn(name = "Họ tên", width = 6000)
    private String fullName;

    @ExcelColumn(name = "Phòng ban", width = 6000)
    private String department;

    @ExcelColumn(name = "Trạng thái", width = 4000)
    public String getStatusExcel() {
        return getIsActive() != null && getIsActive() ? "Hoạt động" : "Khóa";
    }

    @ExcelColumn(name = "Vai trò", width = 7000)
    public String getRolesExcel() {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(", ", roles);
    }

    private Boolean isActive;
    private Set<String> roles;
}
