package com.autopro.backend.dto.vehicle;

import com.autopro.backend.entity.Vehicle;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VehicleResponse {

    private Long id;
    private Long ownerId;
    private String brand;
    private String model;
    private Integer year;
    private String licensePlate;
    private String vin;
    private String color;
    private Integer mileage;
    private LocalDateTime createdAt;

    public static VehicleResponse from(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .ownerId(v.getOwner().getId())
                .brand(v.getBrand())
                .model(v.getModel())
                .year(v.getYear())
                .licensePlate(v.getLicensePlate())
                .vin(v.getVin())
                .color(v.getColor())
                .mileage(v.getMileage())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
