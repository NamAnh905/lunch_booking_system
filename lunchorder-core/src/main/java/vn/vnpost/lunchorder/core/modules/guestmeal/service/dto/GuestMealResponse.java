package vn.vnpost.lunchorder.core.modules.guestmeal.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdBy", "updatedBy" }, ignoreUnknown = true)
public class GuestMealResponse extends BaseResponse {

    private LocalDate mealDate;
    private Long departmentId;
    private String departmentName;
    private Long requestedByUserId;
    private String requestedByFullName;
    private Integer normalQuantity;
    private Integer specialQuantity;
    private BigDecimal normalUnitPrice;
    private BigDecimal specialUnitPrice;
    private BigDecimal totalAmount;
    private String note;
}
