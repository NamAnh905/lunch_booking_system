package vn.vnpost.lunchorder.system.modules.audit.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.system.modules.audit.entity.AuditLog;
import vn.vnpost.lunchorder.system.modules.audit.service.dto.AuditLogResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {

    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    AuditLogResponse toDto(AuditLog auditLog);

    List<AuditLogResponse> toDtoList(List<AuditLog> auditLogs);
}
