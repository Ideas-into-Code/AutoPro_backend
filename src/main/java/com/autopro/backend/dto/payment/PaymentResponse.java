package com.autopro.backend.dto.payment;

import com.autopro.backend.entity.PaymentProvider;
import com.autopro.backend.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private Long serviceRequestId;
    private BigDecimal amount;
    private String currency;
    private PaymentProvider provider;
    private PaymentStatus status;
    private String checkoutUrl;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
