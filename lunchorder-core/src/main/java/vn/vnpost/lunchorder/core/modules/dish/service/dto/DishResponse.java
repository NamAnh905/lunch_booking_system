package vn.vnpost.lunchorder.core.modules.dish.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.common.enums.DishType;
import vn.vnpost.lunchorder.system.modules.excel.annotation.ExcelColumn;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdBy", "updatedBy", "createdAt", "updatedAt" }, ignoreUnknown = true)
public class DishResponse extends BaseResponse {

    @Override
    @ExcelColumn(name = "ID", width = 3000)
    public Long getId() {
        return super.getId();
    }

    @ExcelColumn(name = "Tên món ăn", width = 6000)
    private String name;

    @ExcelColumn(name = "Mô tả", width = 8000)
    private String description;

    @ExcelColumn(name = "Trạng thái", width = 4000)
    public String getStatusExcel() {
        return getIsActive() != null && getIsActive() ? "Hoạt động" : "Khóa";
    }

    @ExcelColumn(name = "Loại món ăn", width = 5000)
    public String getTypeExcel() {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case REGULAR -> "Món thường";
            case SPECIAL -> "Món đặc biệt";
            case DRINK -> "Nước uống";
            case VEGETABLE -> "Rau";
            case SOUP -> "Canh";
            case RICE -> "Cơm";
        };
    }

    private Boolean isActive;
    private DishType type;
}