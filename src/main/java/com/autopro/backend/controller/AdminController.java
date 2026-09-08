package com.autopro.backend.controller;

import com.autopro.backend.dto.admin.AdminStatsDTO;
import com.autopro.backend.dto.admin.MechanicDetailDTO;
import com.autopro.backend.dto.admin.SetUserActiveRequest;
import com.autopro.backend.dto.admin.UserDetailDTO;
import com.autopro.backend.dto.admin.ValidateMechanicRequest;
import com.autopro.backend.entity.ValidationStatus;
import com.autopro.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Administration", description = "Endpoints réservés aux administrateurs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Lister tous les utilisateurs")
    public ResponseEntity<List<UserDetailDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/mechanics")
    @Operation(summary = "Lister les mécaniciens, avec filtre optionnel par statut de validation")
    public ResponseEntity<List<MechanicDetailDTO>> getMechanics(
            @RequestParam(required = false) ValidationStatus status) {
        if (status != null) {
            return ResponseEntity.ok(adminService.getMechanicsByStatus(status));
        }
        return ResponseEntity.ok(adminService.getAllMechanics());
    }

    @GetMapping("/mechanics/pending")
    @Operation(summary = "Lister les mécaniciens en attente de validation")
    public ResponseEntity<List<MechanicDetailDTO>> getPendingMechanics() {
        return ResponseEntity.ok(adminService.getMechanicsByStatus(ValidationStatus.PENDING));
    }

    @PatchMapping("/mechanics/{id}/validate")
    @Operation(summary = "Valider ou rejeter le compte d'un mécanicien")
    public ResponseEntity<MechanicDetailDTO> validateMechanic(
            @PathVariable Long id,
            @Valid @RequestBody ValidateMechanicRequest request) {
        return ResponseEntity.ok(adminService.validateMechanic(id, request.getApproved()));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Suspendre ou réactiver le compte d'un utilisateur")
    public ResponseEntity<UserDetailDTO> setUserActive(
            @PathVariable Long id,
            @Valid @RequestBody SetUserActiveRequest request) {
        return ResponseEntity.ok(adminService.setUserActive(id, request.getActive()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques système")
    public ResponseEntity<AdminStatsDTO> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }
}
