package vn.vnpost.lunchorder.common.audit;

import lombok.Getter;

@Getter
public class AuditEvent {

    private final String action;
    private final String targetEntity;
    private final Long targetId;
    private final Object oldPayload;
    private final Object newPayload;

    public AuditEvent(String action, String targetEntity, Long targetId, Object oldPayload, Object newPayload) {
        this.action = action;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.oldPayload = oldPayload;
        this.newPayload = newPayload;
    }
}
