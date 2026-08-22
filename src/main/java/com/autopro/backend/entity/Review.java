package com.autopro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "reviews",
    indexes = {
        @Index(name = "idx_reviews_mechanic_id", columnList = "mechanic_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_reviews_mechanic_reviewer", columnNames = {"mechanic_id", "reviewer_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_reviews_mechanic_id"))
    private Mechanic mechanic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_reviews_reviewer_id"))
    private User reviewer;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
