package vn.vnpost.lunchorder.core.modules.guestmeal.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GuestMealCreateRequest {

    @NotNull(message = "Ngày ăn không được để trống.")
    private LocalDate mealDate;

    @NotNull(message = "Phòng ban không được để trống.")
    private Long departmentId;

    @NotNull(message = "Người yêu cầu không được để trống.")
    private Long requestedByUserId;

    @NotNull(message = "Số suất thường không được để trống.")
    @Min(value = 0, message = "Số suất thường không được nhỏ hơn 0.")
    @Max(value = 100, message = "Số suất thường không được vượt quá 100.")
    private Integer normalQuantity = 0;

    @Min(value = 0, message = "Số suất đặc biệt không được nhỏ hơn 0.")
    @Max(value = 100, message = "Số suất đặc biệt không được vượt quá 100.")
    private Integer specialQuantity = 0;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
    private String note;
}
