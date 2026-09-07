package com.autopro.backend.dto.servicerequest;

import com.autopro.backend.entity.ProblemType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateServiceRequestRequest {

    @NotBlank(message = "La description est obligatoire")
    private String description;

    /** Type de panne. Défaut : OTHER si absent. */
    private ProblemType problemType;

    /** Numéro de rappel du client. */
    private String contactPhone;

    /** Immobilisation du véhicule. */
    private Boolean isEmergency;

    /** Optionnel : véhicule concerné par la demande */
    private Long vehicleId;

    private String address;

    private Double latitude;

    private Double longitude;
}