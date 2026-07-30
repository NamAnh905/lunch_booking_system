package vn.vnpost.lunchorder.core.modules.order.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.enums.MealType;
import vn.vnpost.lunchorder.core.modules.order.entity.Order;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = MealType.class)
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "departmentName", source = "user.department.name")
    @Mapping(target = "menuId", source = "menu.id")
    @Mapping(target = "menuDate", source = "orderDate")
    @Mapping(target = "isSpecial", expression = "java(order.getMealType() == MealType.SPECIAL)")

    @Mapping(target = "originalUserId", source = "originalUser.id")
    @Mapping(target = "originalUserFullName", source = "originalUser.fullName")
    OrderResponse toDto(Order order);

    List<OrderResponse> toDtoList(List<Order> orders);
}
