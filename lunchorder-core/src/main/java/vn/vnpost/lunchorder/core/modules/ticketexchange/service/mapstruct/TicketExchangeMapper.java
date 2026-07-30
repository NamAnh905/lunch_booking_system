package vn.vnpost.lunchorder.core.modules.ticketexchange.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.enums.MealType;
import vn.vnpost.lunchorder.core.modules.ticketexchange.entity.TicketExchange;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = MealType.class)
public interface TicketExchangeMapper {

    @Mapping(target = "exchangeId", source = "id")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "sellerName", source = "order.originalUser.fullName")
    @Mapping(target = "sellerId", source = "order.originalUser.id")
    @Mapping(target = "menuDate", source = "order.orderDate")
    @Mapping(target = "price", source = "order.price")
    @Mapping(target = "mealType", source = "order.mealType")
    @Mapping(target = "isSpecial", expression = "java(ticketExchange.getOrder() != null && ticketExchange.getOrder().getMealType() == MealType.SPECIAL)")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.fullName")
    TicketExchangeResponse toDto(TicketExchange ticketExchange);

    List<TicketExchangeResponse> toDtoList(List<TicketExchange> ticketExchanges);
}
