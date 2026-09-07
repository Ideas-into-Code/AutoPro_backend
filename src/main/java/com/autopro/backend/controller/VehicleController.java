package com.autopro.backend.controller;

import com.autopro.backend.dto.vehicle.CreateVehicleRequest;
import com.autopro.backend.dto.vehicle.VehicleResponse;
import com.autopro.backend.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Gestion du parc de véhicules de l'utilisateur")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Lister mes véhicules")
    public ResponseEntity<List<VehicleResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(vehicleService.listMine(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un de mes véhicules")
    public ResponseEntity<VehicleResponse> get(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getMine(authentication.getName(), id));
    }

    @PostMapping
    @Operation(summary = "Ajouter un véhicule")
    public ResponseEntity<VehicleResponse> create(
            Authentication authentication, @Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse created = vehicleService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un véhicule")
    public ResponseEntity<VehicleResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un véhicule (refusé s'il est rattaché à une demande en cours)")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        vehicleService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
