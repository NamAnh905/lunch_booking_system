package vn.vnpost.lunchorder.core.modules.menu.service.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import vn.vnpost.lunchorder.common.base.BaseMapper;
import vn.vnpost.lunchorder.core.modules.menu.entity.Menu;
import vn.vnpost.lunchorder.core.modules.dish.service.mapstruct.DishMapper;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuCreateRequest;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuResponse;
import vn.vnpost.lunchorder.core.modules.menu.service.dto.MenuUpdateRequest;
import vn.vnpost.lunchorder.core.modules.price.service.mapstruct.PriceMapper;

@Mapper(componentModel = "spring", uses = {DishMapper.class, PriceMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper extends BaseMapper<MenuCreateRequest, MenuResponse, Menu> {

    @Override
    @Mapping(target = "dishes", ignore = true)
    Menu toEntity(MenuCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "dishes", ignore = true)
    void update(MenuUpdateRequest request, @MappingTarget Menu menu);
}
