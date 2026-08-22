package com.autopro.backend;

import com.autopro.backend.dto.tracking.LocationUpdateDTO;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TrackingService trackingService;

    private User buildUser(Long id) {
        return User.builder().id(id).email("mecano" + id + "@example.com").build();
    }

    private Mechanic buildMechanic(Long id, User user) {
        return Mechanic.builder().id(id).user(user).build();
    }

    private LocationUpdateDTO buildUpdate() {
        return LocationUpdateDTO.builder().latitude(14.7167).longitude(-17.4677).heading(90.0).build();
    }

    @Test
    void publishLocation_broadcastsOnFirstUpdate() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic));

        trackingService.publishLocation(buildUpdate(), user);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/tracking/10"), any(LocationUpdateDTO.class));
    }

    @Test
    void publishLocation_throttlesRapidSuccessiveUpdates() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic));

        trackingService.publishLocation(buildUpdate(), user);
        trackingService.publishLocation(buildUpdate(), user);
        trackingService.publishLocation(buildUpdate(), user);

        // Les appels rapprochés (< 2s) après le premier sont ignorés : un seul broadcast doit partir.
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/tracking/10"), any(LocationUpdateDTO.class));
    }

    @Test
    void publishLocation_broadcastsIndependentlyPerMechanic() {
        User user1 = buildUser(1L);
        User user2 = buildUser(2L);
        Mechanic mechanic1 = buildMechanic(10L, user1);
        Mechanic mechanic2 = buildMechanic(20L, user2);
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic1));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic2));

        trackingService.publishLocation(buildUpdate(), user1);
        trackingService.publishLocation(buildUpdate(), user2);

        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/10"), any(LocationUpdateDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/20"), any(LocationUpdateDTO.class));
    }

    @Test
    void publishLocation_throwsWhenSenderIsNotAMechanic() {
        User user = buildUser(1L);
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.publishLocation(buildUpdate(), user))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(messagingTemplate);
    }
}
