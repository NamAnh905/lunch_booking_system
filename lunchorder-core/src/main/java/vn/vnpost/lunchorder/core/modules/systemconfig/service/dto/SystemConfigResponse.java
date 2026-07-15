package vn.vnpost.lunchorder.core.modules.systemconfig.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SystemConfigResponse {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private Instant updatedAt;
}
