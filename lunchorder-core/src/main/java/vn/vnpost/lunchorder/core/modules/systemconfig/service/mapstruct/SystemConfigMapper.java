package vn.vnpost.lunchorder.core.modules.systemconfig.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.entity.SystemConfig;
import vn.vnpost.lunchorder.core.modules.systemconfig.service.dto.SystemConfigResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemConfigMapper {
    SystemConfigResponse toDto(SystemConfig systemConfig);

    List<SystemConfigResponse> toDtoList(List<SystemConfig> systemConfigs);
}
