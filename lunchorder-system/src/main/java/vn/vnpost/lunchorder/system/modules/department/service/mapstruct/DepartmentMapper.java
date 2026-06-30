package vn.vnpost.lunchorder.system.modules.department.service.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.common.entity.Department;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentCreateRequest;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentResponse;
import vn.vnpost.lunchorder.system.modules.department.service.dto.DepartmentUpdateRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartmentMapper extends BaseMapper<DepartmentCreateRequest, DepartmentResponse, Department> {

    @Override
    Department toEntity(DepartmentCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(DepartmentUpdateRequest request, @MappingTarget Department department);
}
