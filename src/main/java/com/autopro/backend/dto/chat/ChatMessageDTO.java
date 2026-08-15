package com.autopro.backend.dto.chat;

import com.autopro.backend.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String content;
    private ChatMessage.MessageType messageType;
    private LocalDateTime sentAt;
}
