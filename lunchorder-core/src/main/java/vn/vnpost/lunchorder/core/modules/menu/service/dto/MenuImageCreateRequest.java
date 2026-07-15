package vn.vnpost.lunchorder.core.modules.menu.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Yêu cầu tạo thực đơn dạng hình ảnh (theo tuần).
 * {@code weekDate} là một ngày bất kỳ trong tuần; service sẽ chuẩn hóa về Thứ Hai.
 */
@Getter
@Setter
public class MenuImageCreateRequest {

    @NotBlank(message = "Tên menu không được để trống.")
    private String name;

    @NotBlank(message = "Đường dẫn ảnh không được để trống.")
    private String imageUrl;

    @NotNull(message = "Tuần áp dụng không được để trống.")
    private LocalDate weekDate;
}
