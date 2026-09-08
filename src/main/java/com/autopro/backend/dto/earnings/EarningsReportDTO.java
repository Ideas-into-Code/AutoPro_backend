package com.autopro.backend.dto.earnings;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EarningsReportDTO {

    private Long mechanicId;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal dailyEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal monthlyEarnings;
    private long completedInterventions;

    /** Gains jour par jour sur les 7 derniers jours (du plus ancien au plus récent). */
    private List<DailyEarningDTO> dailyBreakdown;
}
