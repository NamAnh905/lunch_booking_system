package vn.vnpost.lunchorder.core.modules.order.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OrderItemRequest {
    @NotNull(message = "Ngày đặt không được để trống.")
    private LocalDate orderDate;

    private Boolean isSpecial = false;
}
