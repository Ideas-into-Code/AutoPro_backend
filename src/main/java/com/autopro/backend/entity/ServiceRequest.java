package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "service_requests",
    indexes = {
        @Index(name = "idx_service_requests_client_id", columnList = "client_id"),
        @Index(name = "idx_service_requests_mechanic_id", columnList = "mechanic_id"),
        @Index(name = "idx_service_requests_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_service_requests_client_id"))
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id",
                foreignKey = @ForeignKey(name = "fk_service_requests_mechanic_id"))
    private Mechanic mechanic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id",
                foreignKey = @ForeignKey(name = "fk_service_requests_vehicle_id"))
    private Vehicle vehicle;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceRequestStatus status = ServiceRequestStatus.PENDING;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}