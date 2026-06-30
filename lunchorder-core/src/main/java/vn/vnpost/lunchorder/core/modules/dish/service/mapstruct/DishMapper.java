package vn.vnpost.lunchorder.core.modules.dish.service.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.common.entity.Dish;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishCreateRequest;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishResponse;
import vn.vnpost.lunchorder.core.modules.dish.service.dto.DishUpdateRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DishMapper extends BaseMapper<DishCreateRequest, DishResponse, Dish> {

    @Override
    Dish toEntity(DishCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(DishUpdateRequest request, @MappingTarget Dish dish);
}
