package vn.vnpost.lunchorder.core.modules.order.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" })
public class OrderResponse extends BaseResponse {
    private Long userId;
    private Long menuId;
    private LocalDate menuDate;
    private BigDecimal price;
    private String status;
    private String ticketSource;
    private Boolean isSpecial;
    private Long originalUserId;
    private Boolean isPrinted;
}
