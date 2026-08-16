package com.autopro.backend.controller;

import com.autopro.backend.dto.earnings.EarningsReportDTO;
import com.autopro.backend.service.EarningsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/mechanic/earnings")
@RequiredArgsConstructor
@Tag(name = "Gains mécanicien", description = "Calcul et reporting des revenus d'un mécanicien")
public class EarningsController {

    private final EarningsService earningsService;

    @GetMapping("/{mechanicId}")
    @PreAuthorize("@earningsService.isOwner(#mechanicId, authentication.name) or hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Rapport de gains journaliers, hebdomadaires et mensuels d'un mécanicien",
               description = "Retourne les gains agrégés pour aujourd'hui, la semaine et le mois courants. "
                           + "Paramètre optionnel `date` (ISO 8601) pour une autre date de référence.")
    public ResponseEntity<EarningsReportDTO> getEarningsReport(
            @PathVariable Long mechanicId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reference = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(earningsService.getEarningsReport(mechanicId, reference));
    }
}
