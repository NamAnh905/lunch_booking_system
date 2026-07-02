package vn.vnpost.lunchorder.core.modules.payment.service.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vn.vnpost.lunchorder.common.entity.Payment;
import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    PaymentResponse toDto(Payment payment);

    List<PaymentResponse> toDtoList(List<Payment> payments);
}
