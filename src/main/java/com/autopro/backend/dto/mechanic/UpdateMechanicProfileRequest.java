package com.autopro.backend.dto.mechanic;

import lombok.Data;

@Data
public class UpdateMechanicProfileRequest {

    private String specialization;
    private Integer experienceYears;
    private String bio;
    private Boolean isAvailable;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String openingHours;
}