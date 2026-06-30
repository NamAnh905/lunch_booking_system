package vn.vnpost.lunchorder.core.modules.menu.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class MenuUpdateRequest {
    @NotNull(message = "Ngày của thực đơn không được để trống.")
    private LocalDate menuDate;

    @NotNull(message = "Giá suất ăn không được để trống.")
    private BigDecimal price;

    private Boolean isSpecial;

    @NotBlank(message = "Trạng thái thực đơn không được để trống.")
    private String status;

    private Set<Long> dishIds;
}
