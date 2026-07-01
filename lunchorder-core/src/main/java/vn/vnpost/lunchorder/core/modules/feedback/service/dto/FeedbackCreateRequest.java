package vn.vnpost.lunchorder.core.modules.feedback.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackCreateRequest {

    @NotNull(message = "ID thực đơn không được để trống.")
    private Long menuId;

    @NotBlank(message = "Ý kiến đóng góp không được để trống.")
    @Size(max = 500, message = "Ý kiến đóng góp không được quá 500 ký tự.")
    private String comment;
}
