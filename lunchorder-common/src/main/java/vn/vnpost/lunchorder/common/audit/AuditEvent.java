package vn.vnpost.lunchorder.common.audit;

import lombok.Getter;

@Getter
public class AuditEvent {

    private final String action;
    private final String targetEntity;
    private final Long targetId;
    private final Object payload;

    public AuditEvent(String action, String targetEntity, Long targetId, Object payload) {
        this.action = action;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.payload = payload;
    }
}
