package com.autopro.backend.service;

import com.autopro.backend.dto.earnings.EarningsReportDTO;
import com.autopro.backend.entity.InterventionStatus;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.InterventionRepository;
import com.autopro.backend.repository.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EarningsService {

    private final InterventionRepository interventionRepository;
    private final MechanicRepository     mechanicRepository;

    /**
     * Calcule les gains journaliers, hebdomadaires et mensuels d'un mécanicien
     * pour la date de référence donnée (par défaut aujourd'hui).
     *
     * @param mechanicId identifiant du mécanicien
     * @param reference  date de référence (utilisée pour définir le jour, la semaine et le mois)
     * @return rapport de gains agrégés
     */
    public EarningsReportDTO getEarningsReport(Long mechanicId, LocalDate reference) {
        // Résolution du mécanicien (lève IllegalArgumentException si introuvable)
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new ResourceNotFoundException("Mécanicien introuvable : " + mechanicId));

        // --- Bornes journalières ---
        LocalDateTime dayStart   = reference.atStartOfDay();
        LocalDateTime dayEnd     = reference.plusDays(1).atStartOfDay();

        // --- Bornes hebdomadaires (lundi–dimanche selon locale FR) ---
        WeekFields weekFields   = WeekFields.of(Locale.FRANCE);
        LocalDate  weekStart    = reference.with(weekFields.dayOfWeek(), 1);
        LocalDate  weekEnd      = weekStart.plusWeeks(1);

        // --- Bornes mensuelles ---
        LocalDate  monthStart   = reference.withDayOfMonth(1);
        LocalDate  monthEnd     = monthStart.plusMonths(1);

        BigDecimal daily   = sumEarnings(mechanic.getId(), dayStart,              dayEnd.minusNanos(1));
        BigDecimal weekly  = sumEarnings(mechanic.getId(), weekStart.atStartOfDay(), weekEnd.atStartOfDay().minusNanos(1));
        BigDecimal monthly = sumEarnings(mechanic.getId(), monthStart.atStartOfDay(), monthEnd.atStartOfDay().minusNanos(1));

        long count = interventionRepository.countByMechanicIdAndStatusAndInterventionDateBetween(
                mechanic.getId(),
                InterventionStatus.COMPLETED,
                monthStart.atStartOfDay(),
                monthEnd.atStartOfDay().minusNanos(1));

        return EarningsReportDTO.builder()
                .mechanicId(mechanic.getId())
                .from(monthStart)
                .to(monthEnd.minusDays(1))
                .dailyEarnings(daily)
                .weeklyEarnings(weekly)
                .monthlyEarnings(monthly)
                .completedInterventions(count)
                .build();
    }

    /**
     * Vérifie que le mécanicien identifié par {@code mechanicId} appartient à l'utilisateur
     * dont l'adresse email est {@code email}. Utilisé dans les expressions SpEL @PreAuthorize.
     */
    public boolean isOwner(Long mechanicId, String email) {
        return mechanicRepository.findById(mechanicId)
                .map(m -> m.getUser().getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }

    private BigDecimal sumEarnings(Long mechanicId, LocalDateTime from, LocalDateTime to) {
        BigDecimal result = interventionRepository.sumAmountByMechanicAndStatusAndDateBetween(
                mechanicId, InterventionStatus.COMPLETED, from, to);
        return result == null ? BigDecimal.ZERO : result;
    }
}
