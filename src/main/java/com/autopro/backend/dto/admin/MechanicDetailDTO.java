package com.autopro.backend.dto.admin;

import com.autopro.backend.entity.Mechanic;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MechanicDetailDTO {

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
    private String validationStatus;
    private LocalDateTime createdAt;

    public static MechanicDetailDTO from(Mechanic mechanic) {
        return MechanicDetailDTO.builder()
                .id(mechanic.getId())
                .userId(mechanic.getUser().getId())
                .firstName(mechanic.getUser().getFirstName())
                .lastName(mechanic.getUser().getLastName())
                .email(mechanic.getUser().getEmail())
                .phone(mechanic.getUser().getPhone())
                .specialization(mechanic.getSpecialization())
                .experienceYears(mechanic.getExperienceYears())
                .bio(mechanic.getBio())
                .isAvailable(mechanic.getIsAvailable())
                .validationStatus(mechanic.getValidationStatus().name())
                .createdAt(mechanic.getCreatedAt())
                .build();
    }
}
