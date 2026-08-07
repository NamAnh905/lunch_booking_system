package vn.vnpost.lunchorder.core.modules.guestmeal.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.core.modules.guestmeal.entity.GuestMeal;
import vn.vnpost.lunchorder.core.modules.guestmeal.service.dto.GuestMealResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GuestMealMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "requestedByUserId", source = "requestedBy.id")
    @Mapping(target = "requestedByFullName", source = "requestedBy.fullName")
    GuestMealResponse toDto(GuestMeal entity);

    List<GuestMealResponse> toDtoList(List<GuestMeal> entityList);
}
