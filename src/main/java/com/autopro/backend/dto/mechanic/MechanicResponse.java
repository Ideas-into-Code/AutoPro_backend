package com.autopro.backend.dto.mechanic;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MechanicResponse {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String specialization;
    private Integer experienceYears;
    private String bio;
    private Boolean isAvailable;
    private Double latitude;
    private Double longitude;

    /** Renseigné uniquement par l'endpoint /nearby */
    private Double distanceKm;
}