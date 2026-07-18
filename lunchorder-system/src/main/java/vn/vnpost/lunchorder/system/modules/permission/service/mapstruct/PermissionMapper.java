package vn.vnpost.lunchorder.system.modules.permission.service.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.system.modules.permission.entity.Permission;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionCreateRequest;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionResponse;
import vn.vnpost.lunchorder.system.modules.permission.service.dto.PermissionUpdateRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper extends BaseMapper<PermissionCreateRequest, PermissionResponse, Permission> {

    @Override
    Permission toEntity(PermissionCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(PermissionUpdateRequest request, @MappingTarget Permission permission);
}
