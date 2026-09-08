package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification destinée à un utilisateur. Créée par les services métier lors
 * d'un événement (demande acceptée, paiement encaissé, nouveau message…) et
 * poussée en temps réel par WebSocket ({@code /user/queue/notifications}).
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient_id", columnList = "recipient_id"),
        @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notifications_recipient_id"))
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    @Builder.Default
    private NotificationType type = NotificationType.GENERAL;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    /** Chemin relatif vers l'écran concerné (« /demandes/12 »), optionnel. */
    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
