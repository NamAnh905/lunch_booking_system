package vn.vnpost.lunchorder.system.modules.auth.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectRequest {
    @NotBlank(message = "Token không được để trống.")
    private String token;
}
