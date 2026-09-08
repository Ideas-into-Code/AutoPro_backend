package com.autopro.backend.service;

import com.autopro.backend.dto.tracking.LocationUpdateDTO;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private static final Duration MIN_BROADCAST_INTERVAL = Duration.ofSeconds(2);

    private final MechanicRepository mechanicRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentMap<Long, Instant> lastBroadcastAt = new ConcurrentHashMap<>();

    @Transactional
    public void publishLocation(LocationUpdateDTO update, User sender) {
        Mechanic mechanic = mechanicRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new IllegalStateException("Only mechanics can broadcast their location"));

        Long mechanicId = mechanic.getId();
        Instant now = Instant.now();

        boolean shouldBroadcast = lastBroadcastAt.compute(mechanicId, (id, lastSent) ->
                (lastSent != null && Duration.between(lastSent, now).compareTo(MIN_BROADCAST_INTERVAL) < 0)
                        ? lastSent
                        : now
        ) == now;

        if (!shouldBroadcast) {
            return;
        }

        // On mémorise la dernière position connue : la recherche géospatiale
        // (`/nearby`) et le premier affichage de la carte de suivi côté client
        // partent de là, avant même que le premier message temps réel n'arrive.
        if (update.getLatitude() != null && update.getLongitude() != null) {
            mechanic.setLatitude(update.getLatitude());
            mechanic.setLongitude(update.getLongitude());
            mechanicRepository.save(mechanic);
        }

        update.setMechanicId(mechanicId);
        update.setTimestamp(now);
        messagingTemplate.convertAndSend("/topic/tracking/" + mechanicId, update);
    }
}
