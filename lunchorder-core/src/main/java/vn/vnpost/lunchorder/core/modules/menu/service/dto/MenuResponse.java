package vn.vnpost.lunchorder.core.modules.menu.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" })
public class MenuResponse extends BaseResponse {
    private LocalDate menuDate;
    private BigDecimal price;
    private Boolean isSpecial;
    private String status;

    @JsonIgnoreProperties(value = { "id", "description", "isActive" })
    private Set<DishResponse> dishes;
}
