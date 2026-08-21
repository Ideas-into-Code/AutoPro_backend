package com.autopro.backend.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {

    /** Optionnel : demande de service concernée par ce paiement. */
    private Long serviceRequestId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal amount;

    @NotBlank
    private String description;
}
