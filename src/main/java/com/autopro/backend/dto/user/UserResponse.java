package com.autopro.backend.dto.user;

import com.autopro.backend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Représentation publique d'un utilisateur, renvoyée par {@code /api/users/me}
 * et imbriquée dans {@link com.autopro.backend.dto.auth.AuthResponse}.
 * Ne contient jamais le mot de passe.
 */
@Data
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private Boolean active;

    /** Id du profil mécanicien si l'utilisateur en a un, sinon {@code null}. */
    private Long mechanicId;

    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        Long mechanicId = user.getMechanic() != null ? user.getMechanic().getId() : null;
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().getName())
                .active(user.getIsActive())
                .mechanicId(mechanicId)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
