package vn.vnpost.lunchorder.core.modules.order.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.core.modules.order.entity.Order;
import vn.vnpost.lunchorder.core.modules.order.service.dto.OrderResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "departmentName", source = "user.department.name")
    @Mapping(target = "roleName", expression = "java(order.getUser() != null && order.getUser().getRoles() != null && !order.getUser().getRoles().isEmpty() ? order.getUser().getRoles().iterator().next().getName() : null)")
    @Mapping(target = "menuId", source = "menu.id")
    @Mapping(target = "menuDate", source = "orderDate")

    @Mapping(target = "originalUserId", source = "originalUser.id")
    @Mapping(target = "originalUserFullName", source = "originalUser.fullName")
    OrderResponse toDto(Order order);

    List<OrderResponse> toDtoList(List<Order> orders);
}
