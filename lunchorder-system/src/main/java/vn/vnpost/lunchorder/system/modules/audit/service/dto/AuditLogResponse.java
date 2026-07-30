package vn.vnpost.lunchorder.system.modules.audit.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class AuditLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String action;
    private String targetEntity;
    private Long targetId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private OffsetDateTime createdAt;
}
