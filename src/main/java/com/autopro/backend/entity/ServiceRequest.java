package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "problem_type", nullable = false, length = 20)
    @Builder.Default
    private ProblemType problemType = ProblemType.OTHER;

    /** Numéro de rappel du client (souvent en panne, injoignable autrement). */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /** Immobilisation : le mécanicien priorise ces demandes. */
    @Column(name = "is_emergency", nullable = false)
    @Builder.Default
    private Boolean isEmergency = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceRequestStatus status = ServiceRequestStatus.PENDING;

    /**
     * Prix convenu de l'intervention, fixé par le mécanicien (ou l'admin) une
     * fois la demande acceptée. Doit être renseigné avant le passage à
     * {@code COMPLETED} : c'est le montant du paiement en espèces.
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /** Photos jointes par le client (URLs Cloudinary). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_request_photos",
            joinColumns = @JoinColumn(name = "service_request_id"))
    @Column(name = "url", length = 500, nullable = false)
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    /** Motif d'annulation, renseigné uniquement quand {@code status == CANCELLED}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 40)
    private CancellationReason cancellationReason;

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