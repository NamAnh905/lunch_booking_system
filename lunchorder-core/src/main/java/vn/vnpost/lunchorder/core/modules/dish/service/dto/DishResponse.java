package vn.vnpost.lunchorder.core.modules.dish.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import vn.vnpost.lunchorder.common.base.BaseResponse;
import vn.vnpost.lunchorder.common.enums.DishType;

@Getter
@Setter
@JsonIgnoreProperties(value = { "createdBy", "updatedBy", "createdAt", "updatedAt" })
public class DishResponse extends BaseResponse {
    private String name;
    private String description;
    private Boolean isActive;
    private DishType type;
}