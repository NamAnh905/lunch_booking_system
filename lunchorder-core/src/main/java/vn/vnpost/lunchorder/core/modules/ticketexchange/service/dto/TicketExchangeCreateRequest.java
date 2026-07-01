package vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketExchangeCreateRequest {
    @NotNull(message = "Mã đơn hàng không được để trống.")
    private Long orderId;
}
