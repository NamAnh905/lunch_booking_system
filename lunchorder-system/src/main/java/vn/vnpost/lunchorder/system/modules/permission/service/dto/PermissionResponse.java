package vn.vnpost.lunchorder.system.modules.permission.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.tools.excel.ExcelColumn;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" }, ignoreUnknown = true)
public class PermissionResponse extends BaseResponse {

    @ExcelColumn(name = "Mã quyền", width = 8000)
    private String action;

    @ExcelColumn(name = "Mô tả", width = 12000)
    private String description;
}
