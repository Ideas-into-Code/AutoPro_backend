package com.autopro.backend.dto.servicerequest;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateServiceRequestRequest {

    @NotBlank(message = "La description est obligatoire")
    private String description;

    /** Optionnel : véhicule concerné par la demande */
    private Long vehicleId;

    private String address;

    private Double latitude;

    private Double longitude;
}