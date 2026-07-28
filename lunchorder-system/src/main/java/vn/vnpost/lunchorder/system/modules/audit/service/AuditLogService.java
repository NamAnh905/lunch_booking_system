package vn.vnpost.lunchorder.system.modules.audit.service;

import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.system.modules.audit.service.dto.AuditLogResponse;

import java.time.LocalDate;
import java.util.List;

public interface AuditLogService {

    void record(Long userId, String action, String targetEntity, Long targetId, String newValue, String ipAddress);

    PageResponse<AuditLogResponse> findAll(String keyword, String action, LocalDate startDate, LocalDate endDate, int page, int size);

    List<String> getActions();
}
