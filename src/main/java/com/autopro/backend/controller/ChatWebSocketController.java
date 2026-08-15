package com.autopro.backend.controller;

import com.autopro.backend.dto.chat.ChatMessageDTO;
import com.autopro.backend.dto.chat.SendMessageRequest;
import com.autopro.backend.dto.chat.TypingNotificationDTO;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Client sends to /app/chat.send
     * Message is broadcast to /topic/chat/{chatRoomId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        User sender = resolveUser(principal);
        chatService.sendMessage(request, sender);
    }

    /**
     * Client sends to /app/chat.typing
     * Typing notification is broadcast to /topic/chat/{chatRoomId}/typing
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingNotificationDTO notification, Principal principal) {
        User user = resolveUser(principal);
        notification.setUserId(user.getId());
        notification.setUserName(user.getFirstName() + " " + user.getLastName());
        chatService.broadcastTyping(notification);
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
