package com.autopro.backend.dto.mechanic;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Bascule de disponibilité du mécanicien. */
@Data
public class UpdateAvailabilityRequest {

    @NotNull(message = "Le champ 'available' est obligatoire")
    private Boolean available;
}
