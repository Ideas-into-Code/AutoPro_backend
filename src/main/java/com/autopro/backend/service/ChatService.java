package com.autopro.backend.service;

import com.autopro.backend.dto.chat.*;
import com.autopro.backend.entity.ChatMessage;
import com.autopro.backend.entity.ChatRoom;
import com.autopro.backend.entity.User;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.ChatMessageRepository;
import com.autopro.backend.repository.ChatRoomRepository;
import com.autopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatRoomDTO createChatRoom(CreateChatRoomRequest request, User creator) {
        List<User> participants = userRepository.findAllById(request.getParticipantIds());
        if (!participants.contains(creator)) {
            participants.add(creator);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .isGroup(request.getIsGroup())
                .participants(participants)
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);
        return toChatRoomDTO(chatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getUserChatRooms(User user) {
        return chatRoomRepository.findByParticipantId(user.getId())
                .stream()
                .map(this::toChatRoomDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDTO sendMessage(SendMessageRequest request, User sender) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType() != null
                        ? request.getMessageType()
                        : ChatMessage.MessageType.CHAT)
                .build();

        message = chatMessageRepository.save(message);
        ChatMessageDTO dto = toChatMessageDTO(message);

        messagingTemplate.convertAndSend("/topic/chat/" + chatRoom.getId(), dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(Long chatRoomId, User requester, int page, int size) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
        boolean isMember = chatRoom.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(requester.getId()));
        if (!isMember) {
            throw new SecurityException("Access denied: user is not a participant of this chat room");
        }
        Page<ChatMessage> messages = chatMessageRepository
                .findByChatRoomIdOrderBySentAtDesc(chatRoomId, PageRequest.of(page, size));
        return messages.getContent().stream()
                .map(this::toChatMessageDTO)
                .collect(Collectors.toList());
    }

    public void broadcastTyping(TypingNotificationDTO notification) {
        messagingTemplate.convertAndSend(
                "/topic/chat/" + notification.getChatRoomId() + "/typing",
                notification);
    }

    private ChatMessageDTO toChatMessageDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .sentAt(message.getSentAt())
                .build();
    }

    private ChatRoomDTO toChatRoomDTO(ChatRoom chatRoom) {
        return ChatRoomDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .isGroup(chatRoom.getIsGroup())
                .participantIds(chatRoom.getParticipants().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()))
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
