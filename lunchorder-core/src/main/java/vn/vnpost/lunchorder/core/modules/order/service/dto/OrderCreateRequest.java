package vn.vnpost.lunchorder.core.modules.order.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateRequest {
    @NotNull(message = "ID thực đơn không được để trống.")
    private Long menuId;
}
