package com.autopro.backend.dto.servicerequest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Prix convenu de l'intervention, fixé par le mécanicien assigné ou l'admin. */
@Data
public class SetPriceRequest {

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être strictement positif")
    @Digits(integer = 8, fraction = 2, message = "Prix invalide")
    private BigDecimal amount;
}
