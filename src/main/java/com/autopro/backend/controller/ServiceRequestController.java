package com.autopro.backend.controller;

import com.autopro.backend.dto.servicerequest.CreateServiceRequestRequest;
import com.autopro.backend.dto.servicerequest.ServiceRequestResponse;
import com.autopro.backend.dto.servicerequest.UpdateServiceRequestStatusRequest;
import com.autopro.backend.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@Tag(name = "Service Requests", description = "Création et suivi des demandes de service")
@SecurityRequirement(name = "bearerAuth")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    @Operation(summary = "Créer une nouvelle demande de service")
    public ResponseEntity<ServiceRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateServiceRequestRequest request) {
        return ResponseEntity.ok(serviceRequestService.create(authentication.getName(), request));
    }

    @GetMapping
    @Operation(summary = "Lister mes demandes de service (selon le rôle : client, mécanicien ou admin)")
    public ResponseEntity<List<ServiceRequestResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.getMyRequests(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une demande de service")
    public ResponseEntity<ServiceRequestResponse> getById(
            Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getById(authentication.getName(), id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'une demande (en attente, acceptée, terminée...)")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequestStatusRequest request) {
        return ResponseEntity.ok(serviceRequestService.updateStatus(authentication.getName(), id, request));
    }
}