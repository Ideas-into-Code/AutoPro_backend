package com.autopro.backend.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRoomRequest {

    private String name;

    @NotNull
    private Boolean isGroup;

    @NotNull
    private java.util.List<Long> participantIds;
}
