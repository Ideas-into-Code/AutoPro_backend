package com.autopro.backend.entity;

/**
 * Motif d'annulation d'une demande de service, choisi dans une liste par la
 * partie qui annule. Sert au suivi qualité (mécanicien trop lent, prix trop
 * élevé…) et évite les annulations sans explication.
 */
public enum CancellationReason {
    NO_LONGER_NEEDED,
    FOUND_ANOTHER_SOLUTION,
    MECHANIC_TOO_SLOW,
    PRICE_TOO_HIGH,
    CREATED_BY_MISTAKE,
    MECHANIC_UNAVAILABLE,
    OTHER
}
