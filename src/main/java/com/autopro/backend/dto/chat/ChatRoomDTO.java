package com.autopro.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {

    private Long id;
    private String name;
    private boolean isGroup;
    private List<Long> participantIds;
    private List<ParticipantSummary> participants;

    /** Aperçu du dernier message, pour la liste des conversations. */
    private String lastMessageContent;
    private Long lastMessageSenderId;
    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;
}
