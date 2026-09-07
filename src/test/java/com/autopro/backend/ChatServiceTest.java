package com.autopro.backend;

import com.autopro.backend.dto.chat.ChatRoomDTO;
import com.autopro.backend.dto.chat.SendMessageRequest;
import com.autopro.backend.entity.ChatMessage;
import com.autopro.backend.entity.ChatRoom;
import com.autopro.backend.entity.Role;
import com.autopro.backend.entity.User;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.ChatMessageRepository;
import com.autopro.backend.repository.ChatRoomRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private ChatService chatService;

    private User user(Long id) {
        return User.builder().id(id).firstName("User").lastName(String.valueOf(id))
                .email("u" + id + "@example.com")
                .role(Role.builder().id(1L).name("ROLE_CLIENT").build()).build();
    }

    private ChatRoom room(Long id, User... members) {
        List<User> list = new ArrayList<>(List.of(members));
        return ChatRoom.builder().id(id).isGroup(false).participants(list).build();
    }

    @Test
    void sendMessage_rejectsNonParticipant() {
        User a = user(1L), b = user(2L), intruder = user(9L);
        ChatRoom room = room(5L, a, b);
        when(chatRoomRepository.findById(5L)).thenReturn(Optional.of(room));

        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(5L);
        req.setContent("hello");

        assertThatThrownBy(() -> chatService.sendMessage(req, intruder))
                .isInstanceOf(SecurityException.class);
        verify(chatMessageRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void sendMessage_savesAndBroadcastsForParticipant() {
        User a = user(1L), b = user(2L);
        ChatRoom room = room(5L, a, b);
        when(chatRoomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(5L);
        req.setContent("bonjour");

        var dto = chatService.sendMessage(req, a);

        assertThat(dto.getContent()).isEqualTo("bonjour");
        assertThat(dto.getSenderId()).isEqualTo(1L);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/5"), any(Object.class));
    }

    @Test
    void getOrCreateDirectRoom_returnsExistingWhenFound() {
        User me = user(1L), peer = user(2L);
        ChatRoom existing = room(7L, me, peer);
        when(userRepository.findById(2L)).thenReturn(Optional.of(peer));
        when(chatRoomRepository.findPrivateChatRoom(1L, 2L)).thenReturn(Optional.of(existing));
        when(chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(7L)).thenReturn(Optional.empty());

        ChatRoomDTO dto = chatService.getOrCreateDirectRoom(me, 2L);

        assertThat(dto.getId()).isEqualTo(7L);
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void getOrCreateDirectRoom_createsWhenNoneExists() {
        User me = user(1L), peer = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(peer));
        when(chatRoomRepository.findPrivateChatRoom(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(inv -> {
            ChatRoom r = inv.getArgument(0);
            r.setId(8L);
            return r;
        });
        when(chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(8L)).thenReturn(Optional.empty());

        ChatRoomDTO dto = chatService.getOrCreateDirectRoom(me, 2L);

        assertThat(dto.getId()).isEqualTo(8L);
        assertThat(dto.getParticipantIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void getOrCreateDirectRoom_rejectsSelf() {
        User me = user(1L);
        assertThatThrownBy(() -> chatService.getOrCreateDirectRoom(me, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getOrCreateDirectRoom_throwsWhenPeerMissing() {
        User me = user(1L);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> chatService.getOrCreateDirectRoom(me, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getChatHistory_deniesNonParticipant() {
        User a = user(1L), b = user(2L), intruder = user(9L);
        ChatRoom room = room(5L, a, b);
        when(chatRoomRepository.findById(5L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.getChatHistory(5L, intruder, 0, 50))
                .isInstanceOf(SecurityException.class);
    }
}
