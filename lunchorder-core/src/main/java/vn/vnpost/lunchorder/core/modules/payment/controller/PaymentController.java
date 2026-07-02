package vn.vnpost.lunchorder.core.modules.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.modules.payment.service.PaymentService;
import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentCreateRequest;
import vn.vnpost.lunchorder.core.modules.payment.service.dto.PaymentResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PAYMENTS')")
    public ApiResponse<PaymentResponse> createPayment(@RequestBody @Valid PaymentCreateRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPayment(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ApiResponse<List<PaymentResponse>> getPayments(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        return ApiResponse.<List<PaymentResponse>>builder()
                .result(paymentService.getPayments(userId, month, year))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENTS')")
    public ApiResponse<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ApiResponse.<Void>builder()
                .message("Payment deleted successfully")
                .build();
    }
}
