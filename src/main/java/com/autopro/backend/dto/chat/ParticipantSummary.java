package com.autopro.backend.dto.chat;

import com.autopro.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un participant d'une conversation, réduit à ce que l'interface affiche. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSummary {

    private Long id;
    private String fullName;
    private String role;

    public static ParticipantSummary from(User user) {
        return ParticipantSummary.builder()
                .id(user.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .build();
    }
}
