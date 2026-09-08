package com.autopro.backend.entity;

/** Nature d'une notification. Sert au frontend à choisir l'icône et le ton. */
public enum NotificationType {
    REQUEST_ACCEPTED,
    REQUEST_IN_PROGRESS,
    REQUEST_COMPLETED,
    REQUEST_CANCELLED,
    PAYMENT_COLLECTED,
    NEW_MESSAGE,
    MECHANIC_VALIDATED,
    GENERAL
}
