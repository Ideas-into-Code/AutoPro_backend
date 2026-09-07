package com.autopro.backend.entity;

/**
 * Cycle de vie d'un paiement.
 *
 * <ul>
 *   <li>{@link #PENDING} : montant dû, en attente de règlement. Créé
 *       automatiquement quand la demande passe à {@code COMPLETED}.</li>
 *   <li>{@link #COLLECTED} : le mécanicien a confirmé avoir reçu les espèces.</li>
 *   <li>{@link #CANCELLED} : demande annulée avant règlement, le paiement est
 *       caduc.</li>
 * </ul>
 */
public enum PaymentStatus {
    PENDING,
    COLLECTED,
    CANCELLED
}
