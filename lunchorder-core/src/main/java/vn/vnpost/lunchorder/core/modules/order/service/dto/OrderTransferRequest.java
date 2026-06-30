package vn.vnpost.lunchorder.core.modules.order.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderTransferRequest {
    @NotNull(message = "ID người dùng nhận chuyển nhượng không được để trống.")
    private Long targetUserId;

    private String note;
}
