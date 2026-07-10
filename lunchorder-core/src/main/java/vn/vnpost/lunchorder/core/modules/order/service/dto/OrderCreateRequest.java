package vn.vnpost.lunchorder.core.modules.order.service.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class OrderCreateRequest {
    @NotEmpty(message = "Danh sách ngày đặt không được để trống.")
    private Set<LocalDate> orderDates;
}
