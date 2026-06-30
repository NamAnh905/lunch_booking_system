package vn.vnpost.lunchorder.core.modules.order.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequest {
    @NotBlank(message = "Trạng thái không được để trống.")
    private String status;
}
