package vn.vnpost.lunchorder.core.modules.payment.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentCreateRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "paymentMethod is required")
    private String paymentMethod;

    @NotNull(message = "paymentMonth is required")
    @Min(value = 1, message = "paymentMonth must be between 1 and 12")
    @Max(value = 12, message = "paymentMonth must be between 1 and 12")
    private Integer paymentMonth;

    @NotNull(message = "paymentYear is required")
    @Min(value = 2020, message = "paymentYear is invalid")
    private Integer paymentYear;

    private String note;
}
