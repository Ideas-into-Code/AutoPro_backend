package com.autopro.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingNotificationDTO {

    private Long chatRoomId;
    private Long userId;
    private String userName;
    private boolean typing;
}
