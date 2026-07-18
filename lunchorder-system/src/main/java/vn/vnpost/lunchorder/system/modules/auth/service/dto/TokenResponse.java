package vn.vnpost.lunchorder.system.modules.auth.service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String token;
    private boolean authenticated;
    private boolean rememberMe;
}
