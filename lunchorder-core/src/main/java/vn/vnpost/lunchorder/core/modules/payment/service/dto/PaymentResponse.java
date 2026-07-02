package vn.vnpost.lunchorder.core.modules.payment.service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private BigDecimal amount;
    private String paymentMethod;
    private Integer paymentMonth;
    private Integer paymentYear;
    private String note;
    private Instant paidAt;
    private Instant createdAt;
}
