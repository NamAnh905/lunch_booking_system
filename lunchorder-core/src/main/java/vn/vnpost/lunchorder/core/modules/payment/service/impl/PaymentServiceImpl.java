package vn.vnpost.lunchorder.core.modules.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vnpost.lunchorder.core.modules.payment.entity.Payment;
import vn.vnpost.lunchorder.system.modules.user.entity.User;
import vn.vnpost.lunchorder.common.enums.PaymentMethod;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.payment.repository.PaymentRepository;
import vn.vnpost.lunchorder.core.modules.payment.service.PaymentService;
import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentCreateRequest;
import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentResponse;
import vn.vnpost.lunchorder.core.modules.payment.service.mapstruct.PaymentMapper;
import vn.vnpost.lunchorder.system.modules.user.service.UserLookupService;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserLookupService userLookupService;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        User user = userLookupService.getById(request.getUserId());

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ENUM_VALUE);
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentMonth(request.getPaymentMonth());
        payment.setPaymentYear(request.getPaymentYear());
        payment.setNote(request.getNote());
        payment.setPaidAt(Instant.now());

        payment = paymentRepository.save(payment);
        log.info("Payment created: id={}, userId={}, amount={}, month={}/{}",
                payment.getId(), user.getId(), payment.getAmount(),
                payment.getPaymentMonth(), payment.getPaymentYear());

        return paymentMapper.toDto(payment);
    }

    @Override
    public List<PaymentResponse> getPayments(Long userId, Integer month, Integer year) {
        List<Payment> payments = paymentRepository.findByFilters(userId, month, year);
        return paymentMapper.toDtoList(payments);
    }

    @Override
    @Transactional
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment deleted: id={}, userId={}, amount={}, month={}/{}",
                payment.getId(), payment.getUser().getId(), payment.getAmount(),
                payment.getPaymentMonth(), payment.getPaymentYear());

        paymentRepository.delete(payment);
    }
}
