package vn.vnpost.lunchorder.system.modules.auth.service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectResponse {
    private boolean valid;
}
