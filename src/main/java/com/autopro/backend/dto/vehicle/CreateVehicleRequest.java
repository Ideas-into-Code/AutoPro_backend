package com.autopro.backend.dto.vehicle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVehicleRequest {

    @NotBlank(message = "La marque est obligatoire")
    @Size(max = 100)
    private String brand;

    @NotBlank(message = "Le modèle est obligatoire")
    @Size(max = 100)
    private String model;

    @NotNull(message = "L'année est obligatoire")
    @Min(value = 1886, message = "Année invalide")
    private Integer year;

    @NotBlank(message = "L'immatriculation est obligatoire")
    @Size(max = 20)
    private String licensePlate;

    @Size(max = 17, message = "Le VIN fait 17 caractères maximum")
    private String vin;

    @Size(max = 50)
    private String color;

    @Min(value = 0, message = "Le kilométrage ne peut être négatif")
    private Integer mileage;
}
