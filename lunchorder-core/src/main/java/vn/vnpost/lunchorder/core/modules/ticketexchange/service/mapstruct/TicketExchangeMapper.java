package vn.vnpost.lunchorder.core.modules.ticketexchange.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.entity.TicketExchange;
import vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto.TicketExchangeResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketExchangeMapper {

    @Mapping(target = "exchangeId", source = "id")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "sellerName", source = "order.originalUser.fullName")
    @Mapping(target = "sellerId", source = "order.originalUser.id")
    @Mapping(target = "menuDate", source = "order.menu.menuDate")
    @Mapping(target = "isSpecial", source = "order.menu.isSpecial")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.fullName")
    TicketExchangeResponse toDto(TicketExchange ticketExchange);

    List<TicketExchangeResponse> toDtoList(List<TicketExchange> ticketExchanges);
}
