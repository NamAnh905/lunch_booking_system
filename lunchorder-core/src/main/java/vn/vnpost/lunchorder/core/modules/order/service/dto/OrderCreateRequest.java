package vn.vnpost.lunchorder.core.modules.order.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateRequest {
    @NotEmpty(message = "Danh sách ngày đặt không được để trống.")
    @Valid
    private List<OrderItemRequest> orders;
}
