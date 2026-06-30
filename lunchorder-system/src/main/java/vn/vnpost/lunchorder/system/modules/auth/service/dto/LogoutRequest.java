package vn.vnpost.lunchorder.system.modules.auth.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "Token không được để trống.")
    private String token;
}
