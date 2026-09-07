package com.autopro.backend.entity;

/**
 * Nature de la panne déclarée par le client. Sert au mécanicien à jauger
 * l'intervention avant de l'accepter et à filtrer les demandes par spécialité.
 * {@link #OTHER} couvre tout ce qui ne rentre pas dans les catégories connues
 * (climatisation, électricité…).
 */
public enum ProblemType {
    BATTERY,
    TIRE,
    ENGINE,
    BRAKES,
    TOWING,
    OTHER
}
