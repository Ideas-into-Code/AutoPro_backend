package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_service_request_id", columnList = "service_request_id"),
        @Index(name = "idx_payments_payer_id", columnList = "payer_id"),
        @Index(name = "idx_payments_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference "molle" vers service_requests.id (pas de FK/relation JPA) : cette table
     * vit sur une autre branche pas encore fusionnee. A relier avec un vrai @ManyToOne
     * une fois feat/mechanic_services mergee dans develop.
     */
    @Column(name = "service_request_id")
    private Long serviceRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_payments_payer_id"))
    private User payer;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XOF";

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "provider_reference", nullable = false, unique = true, length = 100)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
