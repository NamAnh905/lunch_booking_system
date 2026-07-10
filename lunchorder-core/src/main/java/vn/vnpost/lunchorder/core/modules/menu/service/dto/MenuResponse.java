package vn.vnpost.lunchorder.core.modules.menu.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;
import vn.vnpost.lunchorder.system.modules.excel.annotation.ExcelColumn;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" }, ignoreUnknown = true)
public class MenuResponse extends BaseResponse {
    
    @Override
    @ExcelColumn(name = "ID", width = 3000)
    public Long getId() {
        return super.getId();
    }

    @ExcelColumn(name = "Ngày áp dụng", width = 4500)
    public String getMenuDateExcel() {
        if (menuDate == null) {
            return "";
        }
        return menuDate.toString();
    }

    @ExcelColumn(name = "Suất ăn", width = 5000)
    public String getPriceExcel() {
        return price != null ? price.getName() : "";
    }

    @ExcelColumn(name = "Danh sách món ăn", width = 12000)
    public String getDishesExcel() {
        if (dishes == null || dishes.isEmpty()) {
            return "";
        }
        return dishes.stream()
                .filter(java.util.Objects::nonNull)
                .map(DishResponse::getName)
                .collect(Collectors.joining(", "));
    }

    @ExcelColumn(name = "Trạng thái", width = 4000)
    public String getStatusExcel() {
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "Hoạt động";
        }
        return "Khóa";
    }

    private LocalDate menuDate;
    private PriceResponse price;
    private String status;

    @JsonIgnoreProperties(value = { "description", "isActive" }, ignoreUnknown = true)
    private List<DishResponse> dishes;
}
