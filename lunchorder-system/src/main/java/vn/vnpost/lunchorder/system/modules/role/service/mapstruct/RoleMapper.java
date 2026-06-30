package vn.vnpost.lunchorder.system.modules.role.service.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.common.entity.Role;
import vn.vnpost.lunchorder.system.modules.permission.service.mapstruct.PermissionMapper;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleCreateRequest;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleResponse;
import vn.vnpost.lunchorder.system.modules.role.service.dto.RoleUpdateRequest;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper extends BaseMapper<RoleCreateRequest, RoleResponse, Role> {

    @Override
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "permissions", ignore = true)
    void update(RoleUpdateRequest request, @MappingTarget Role role);
}
