package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "mechanics",
    indexes = {
        @Index(name = "idx_mechanics_user_id", columnList = "user_id"),
        @Index(name = "idx_mechanics_is_available", columnList = "is_available")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_mechanics_user_id"))
    private User user;

    @Column(name = "specialization", length = 150)
    private String specialization;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    @Builder.Default
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // La colonne "location" (geography Point, PostGIS) n'est pas mappée ici : elle est
    // tenue synchronisée par un trigger SQL (V16) à partir de latitude/longitude et n'est
    // utilisée que dans la requête native findNearby(). Volontairement pas de dépendance
    // hibernate-spatial/JTS puisque le code applicatif ne manipule jamais de géométrie.
}
