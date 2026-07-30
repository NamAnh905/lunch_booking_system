package vn.vnpost.lunchorder.core.modules.order.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.common.enums.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(value = { "updatedAt", "createdBy", "updatedBy" }, ignoreUnknown = true)
public class OrderResponse extends BaseResponse {
    private Long userId;
    private Long menuId;
    private LocalDate menuDate;
    private BigDecimal price;
    private MealType mealType;
    private Boolean isSpecial;
    private String status;
    private String ticketSource;
    private Long originalUserId;
    private String originalUserFullName;
    private Boolean isPrinted;
    private String errorMessage;
    
    private String userName;
    private String fullName;
    private String departmentName;
}
