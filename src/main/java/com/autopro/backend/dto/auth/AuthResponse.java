package com.autopro.backend.dto.auth;

import com.autopro.backend.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse d'authentification : le token JWT et l'utilisateur associé.
 * Le frontend n'a donc pas besoin d'un second appel à {@code /api/users/me}
 * juste après la connexion.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;

    /** Toujours "Bearer". */
    private String type;

    private UserResponse user;
}
