package com.autopro.backend.controller;

import com.autopro.backend.dto.tracking.LocationUpdateDTO;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class TrackingWebSocketController {

    private final TrackingService trackingService;
    private final UserRepository userRepository;

    /**
     * Client (mécanicien) sends to /app/tracking.update
     * Location is broadcast to /topic/tracking/{mechanicId}
     */
    @MessageMapping("/tracking.update")
    public void updateLocation(@Payload LocationUpdateDTO update, Principal principal) {
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        trackingService.publishLocation(update, sender);
    }
}
