package com.autopro.backend.controller;

import com.autopro.backend.dto.chat.ChatMessageDTO;
import com.autopro.backend.dto.chat.ChatRoomDTO;
import com.autopro.backend.dto.chat.CreateChatRoomRequest;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat room management and message history")
public class ChatRestController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @Operation(summary = "Create a new chat room")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDTO> createRoom(@Valid @RequestBody CreateChatRoomRequest request,
                                                   Principal principal) {
        User creator = resolveUser(principal);
        return ResponseEntity.ok(chatService.createChatRoom(request, creator));
    }

    @Operation(summary = "List all chat rooms for the authenticated user")
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> listRooms(Principal principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(chatService.getUserChatRooms(user));
    }

    @Operation(summary = "Get message history for a chat room")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Long roomId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size,
                                                             Principal principal) {
        return ResponseEntity.ok(chatService.getChatHistory(roomId, resolveUser(principal), page, size));
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
