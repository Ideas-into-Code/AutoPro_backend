package com.autopro.backend.controller;

import com.autopro.backend.dto.mechanic.MechanicResponse;
import com.autopro.backend.dto.mechanic.UpdateAvailabilityRequest;
import com.autopro.backend.dto.mechanic.UpdateMechanicProfileRequest;
import com.autopro.backend.service.MechanicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mechanics")
@RequiredArgsConstructor
@Tag(name = "Mechanics", description = "Recherche géospatiale et gestion des profils mécaniciens")
@SecurityRequirement(name = "bearerAuth")
public class MechanicController {

    private final MechanicService mechanicService;

    @GetMapping
    @Operation(summary = "Lister les mécaniciens (filtrage optionnel par spécialité)")
    public ResponseEntity<List<MechanicResponse>> getAll(
            @RequestParam(required = false) String specialization) {
        return ResponseEntity.ok(mechanicService.getAll(specialization));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Rechercher les mécaniciens proches (GPS), triés par distance")
    public ResponseEntity<List<MechanicResponse>> getNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "20") double radiusKm,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "true") boolean onlyAvailable) {
        return ResponseEntity.ok(mechanicService.findNearby(
                latitude, longitude, radiusKm, specialization, onlyAvailable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le profil d'un mécanicien par son id")
    public ResponseEntity<MechanicResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mechanicService.getById(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_MECHANIC')")
    @Operation(summary = "Obtenir mon propre profil mécanicien")
    public ResponseEntity<MechanicResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(mechanicService.getMyProfile(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_MECHANIC')")
    @Operation(summary = "Mettre à jour mon propre profil mécanicien")
    public ResponseEntity<MechanicResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMechanicProfileRequest request) {
        return ResponseEntity.ok(mechanicService.updateMyProfile(authentication.getName(), request));
    }

    @PatchMapping("/me/availability")
    @PreAuthorize("hasAuthority('ROLE_MECHANIC')")
    @Operation(summary = "Basculer ma disponibilité (profil validé requis pour se rendre disponible)")
    public ResponseEntity<MechanicResponse> updateAvailability(
            Authentication authentication,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(
                mechanicService.updateAvailability(authentication.getName(), request.getAvailable()));
    }
}