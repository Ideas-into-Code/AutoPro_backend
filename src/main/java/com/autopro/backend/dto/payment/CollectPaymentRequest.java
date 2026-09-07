package com.autopro.backend.dto.payment;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Confirmation par le mécanicien qu'il a reçu les espèces.
 * {@code notes} est optionnel (ex. « payé, appoint rendu »).
 */
@Data
public class CollectPaymentRequest {

    @Size(max = 500)
    private String notes;
}
