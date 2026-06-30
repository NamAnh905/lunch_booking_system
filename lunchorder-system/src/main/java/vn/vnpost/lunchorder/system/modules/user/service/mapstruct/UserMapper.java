package vn.vnpost.lunchorder.system.modules.user.service.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.common.entity.User;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserCreateRequest;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserResponse;
import vn.vnpost.lunchorder.system.modules.user.service.dto.UserUpdateRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper extends BaseMapper<UserCreateRequest, UserResponse, User> {

    @Override
    @Mapping(target = "department", source = "department.name")
    UserResponse toDto(User entity);

    @Override
    @Mapping(target = "department", ignore = true)
    User toEntity(UserCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "department", ignore = true)
    void update(UserUpdateRequest request, @MappingTarget User user);
}