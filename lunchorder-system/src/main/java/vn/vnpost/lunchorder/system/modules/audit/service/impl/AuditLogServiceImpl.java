package vn.vnpost.lunchorder.system.modules.audit.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.common.base.PageResponse;
import vn.vnpost.lunchorder.common.constant.PaginationConstants;
import vn.vnpost.lunchorder.system.modules.audit.entity.AuditLog;
import vn.vnpost.lunchorder.system.modules.audit.repository.AuditLogRepository;
import vn.vnpost.lunchorder.system.modules.audit.service.AuditLogService;
import vn.vnpost.lunchorder.system.modules.audit.service.dto.AuditLogResponse;
import vn.vnpost.lunchorder.system.modules.audit.service.mapstruct.AuditLogMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String action, String targetEntity, Long targetId, String oldValue, String newValue, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setTargetEntity(targetEntity);
        auditLog.setTargetId(targetId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }

    @Override
    public PageResponse<AuditLogResponse> findAll(String keyword, String action, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PaginationConstants.toPageable(page, size);

        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startDateTime = (startDate != null ? startDate : MIN_DATE).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endDateTime = (endDate != null ? endDate : MAX_DATE).plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String normalizedAction = action == null ? "" : action.trim();

        Page<AuditLog> auditLogPage = auditLogRepository.search(
                "%" + normalizedKeyword + "%", normalizedAction, startDateTime, endDateTime, pageable);

        return PageResponse.of(auditLogPage, auditLogMapper.toDtoList(auditLogPage.getContent()), page, size);
    }

    @Override
    public List<String> getActions() {
        return auditLogRepository.findDistinctActions();
    }
}
