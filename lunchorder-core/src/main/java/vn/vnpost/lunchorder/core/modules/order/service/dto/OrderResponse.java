package vn.vnpost.lunchorder.core.modules.order.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;

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
    private String status;
    private String ticketSource;
    public Boolean getIsSpecial() {
        return price != null && price.compareTo(new BigDecimal("25000")) > 0;
    }
    private Long originalUserId;
    private Boolean isPrinted;
    private String errorMessage;
    
    // User Info added for UI
    private String userName;
    private String fullName;
    private String roleName;
    private String departmentName;
}
