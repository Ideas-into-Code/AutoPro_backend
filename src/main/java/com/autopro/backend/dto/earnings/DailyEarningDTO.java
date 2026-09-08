package com.autopro.backend.dto.earnings;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gains d'un jour, pour l'historique en barres du tableau de bord. */
@Getter
@AllArgsConstructor
public class DailyEarningDTO {
    private LocalDate date;
    private BigDecimal amount;
}
