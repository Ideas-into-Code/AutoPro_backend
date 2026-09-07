package com.autopro.backend.dto.mechanic;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MechanicResponse {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;

    /** Nom complet, pour éviter au frontend de le recomposer. */
    private String fullName;

    private String email;
    private String phone;
    private String specialization;
    private Integer experienceYears;
    private String bio;
    private Boolean isAvailable;

    /** PENDING, APPROVED ou REJECTED. Seul APPROVED vaut « vérifié ». */
    private String validationStatus;

    private BigDecimal averageRating;
    private Integer reviewCount;

    private Double latitude;
    private Double longitude;

    /** Renseigné uniquement par l'endpoint /nearby */
    private Double distanceKm;
}