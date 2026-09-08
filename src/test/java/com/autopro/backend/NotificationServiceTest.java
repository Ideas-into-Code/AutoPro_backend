package com.autopro.backend;

import com.autopro.backend.dto.notification.NotificationResponse;
import com.autopro.backend.entity.Notification;
import com.autopro.backend.entity.NotificationType;
import com.autopro.backend.entity.User;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.NotificationRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationService notificationService;

    private User user(Long id) {
        return User.builder().id(id).email("u" + id + "@example.com").firstName("A").lastName("B").build();
    }

    @Test
    void notify_savesAndPushesToRecipientQueue() {
        User u = user(1L);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(9L);
            return n;
        });

        notificationService.notify(u, NotificationType.PAYMENT_COLLECTED, "Paiement", "15000 FCFA", "/demandes/2");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PAYMENT_COLLECTED);
        assertThat(captor.getValue().getRead()).isFalse();
        verify(messagingTemplate).convertAndSendToUser(
                eq("u1@example.com"), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    void notify_isNoOpWhenRecipientNull() {
        notificationService.notify(null, NotificationType.GENERAL, "x", "y", null);
        verifyNoInteractions(notificationRepository, messagingTemplate);
    }

    @Test
    void notify_swallowsRepositoryFailure() {
        User u = user(1L);
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("db down"));
        // ne doit pas propager
        notificationService.notify(u, NotificationType.GENERAL, "x", "y", null);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void markRead_deniesNotificationOfAnotherUser() {
        Notification n = Notification.builder().id(5L).recipient(user(2L)).title("t").build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> notificationService.markRead("u1@example.com", 5L))
                .isInstanceOf(SecurityException.class);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_setsReadForOwner() {
        User owner = user(1L);
        Notification n = Notification.builder().id(5L).recipient(owner).title("t").read(false).build();
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(owner));

        notificationService.markRead("u1@example.com", 5L);

        assertThat(n.getRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void markRead_throwsWhenMissing() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markRead("u1@example.com", 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
