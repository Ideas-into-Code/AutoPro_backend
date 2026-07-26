package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "vehicles",
    indexes = {
        @Index(name = "idx_vehicles_owner_id", columnList = "owner_id"),
        @Index(name = "idx_vehicles_license_plate", columnList = "license_plate"),
        @Index(name = "idx_vehicles_vin", columnList = "vin")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_vehicles_owner_id"))
    private User owner;

    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "vin", unique = true, length = 17)
    private String vin;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "mileage")
    private Integer mileage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
