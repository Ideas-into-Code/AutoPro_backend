package com.autopro.backend.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Ouverture (ou récupération) d'une conversation privée avec un autre utilisateur. */
@Data
public class CreateDirectRoomRequest {

    @NotNull(message = "L'identifiant du correspondant est obligatoire")
    private Long peerId;
}
