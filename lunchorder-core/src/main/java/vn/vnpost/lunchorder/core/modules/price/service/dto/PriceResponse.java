package vn.vnpost.lunchorder.core.modules.price.service.dto;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceResponse extends BaseResponse {
    private String name;
    private BigDecimal amount;
    private String description;
    private Boolean isActive;
}
