package vn.vnpost.lunchorder.core.modules.payment.service;

import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentCreateRequest;
import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(PaymentCreateRequest request);

    List<PaymentResponse> getPayments(Long userId, Integer month, Integer year);

    void deletePayment(Long paymentId);
}
