package com.autopro.backend.entity;

/**
 * Moyens de paiement acceptés.
 *
 * <p>Un seul moyen est disponible aujourd'hui : {@link #CASH} (espèces, remises
 * en main propre au mécanicien). L'énumération existe pour que l'ajout d'un
 * futur moyen (mobile money, carte…) se limite à une nouvelle constante et au
 * code de sa passerelle, sans toucher au modèle {@link Payment} ni aux
 * endpoints existants.</p>
 */
public enum PaymentMethod {
    CASH
}
