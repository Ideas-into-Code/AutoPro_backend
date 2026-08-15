package com.autopro.backend.dto.chat;

import com.autopro.backend.entity.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull
    private Long chatRoomId;

    @NotBlank
    private String content;

    private ChatMessage.MessageType messageType = ChatMessage.MessageType.CHAT;
}
