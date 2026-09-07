package com.autopro.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Mise à jour du profil de l'utilisateur courant.
 * L'email et le rôle ne sont pas modifiables ici : l'email sert d'identifiant
 * de connexion, le rôle relève de l'administration.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Size(max = 20)
    private String phone;
}
