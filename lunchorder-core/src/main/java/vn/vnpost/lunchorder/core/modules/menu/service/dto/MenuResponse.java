package vn.vnpost.lunchorder.core.modules.menu.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.price.service.dto.PriceResponse;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdAt", "updatedAt", "createdBy", "updatedBy" })
public class MenuResponse extends BaseResponse {
    private LocalDate menuDate;
    private PriceResponse price;
    private String status;

    @JsonIgnoreProperties(value = { "description", "isActive" })
    private Set<DishResponse> dishes;
}
