package vn.vnpost.lunchorder.core.modules.order.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.entity.Order;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "menuId", source = "menu.id")
    @Mapping(target = "menuDate", source = "menu.menuDate")
    @Mapping(target = "isSpecial", source = "menu.isSpecial")
    @Mapping(target = "originalUserId", source = "originalUser.id")
    OrderResponse toDto(Order order);

    List<OrderResponse> toDtoList(List<Order> orders);
}
