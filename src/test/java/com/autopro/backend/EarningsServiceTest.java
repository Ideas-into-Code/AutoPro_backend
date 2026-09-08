package com.autopro.backend;

import com.autopro.backend.dto.earnings.EarningsReportDTO;
import com.autopro.backend.entity.*;
import com.autopro.backend.repository.InterventionRepository;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.service.EarningsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarningsServiceTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @InjectMocks
    private EarningsService earningsService;

    private Mechanic buildMechanic(Long id) {
        User user = User.builder()
                .id(id)
                .firstName("Ahmed")
                .lastName("Ben Ali")
                .email("ahmed" + id + "@example.com")
                .password("hashed")
                .role(Role.builder().name("ROLE_MECHANIC").build())
                .build();
        return Mechanic.builder()
                .id(id)
                .user(user)
                .specialization("Moteur")
                .validationStatus(ValidationStatus.APPROVED)
                .build();
    }

    @Test
    void getEarningsReport_returnsAggregatedEarnings() {
        Mechanic mechanic = buildMechanic(1L);
        LocalDate reference = LocalDate.of(2024, 6, 12); // Wednesday

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));

        // daily
        when(interventionRepository.sumAmountByMechanicAndStatusAndDateBetween(
                eq(1L), eq(InterventionStatus.COMPLETED),
                eq(reference.atStartOfDay()),
                any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("80.00"));

        // weekly
        when(interventionRepository.sumAmountByMechanicAndStatusAndDateBetween(
                eq(1L), eq(InterventionStatus.COMPLETED),
                eq(LocalDate.of(2024, 6, 10).atStartOfDay()),
                any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("350.00"));

        // monthly
        when(interventionRepository.sumAmountByMechanicAndStatusAndDateBetween(
                eq(1L), eq(InterventionStatus.COMPLETED),
                eq(LocalDate.of(2024, 6, 1).atStartOfDay()),
                any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1200.00"));

        // interventions for count
        when(interventionRepository.countByMechanicIdAndStatusAndInterventionDateBetween(
                eq(1L), eq(InterventionStatus.COMPLETED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);

        EarningsReportDTO report = earningsService.getEarningsReport(1L, reference);

        assertThat(report.getMechanicId()).isEqualTo(1L);
        assertThat(report.getDailyEarnings()).isEqualByComparingTo("80.00");
        assertThat(report.getWeeklyEarnings()).isEqualByComparingTo("350.00");
        assertThat(report.getMonthlyEarnings()).isEqualByComparingTo("1200.00");
        assertThat(report.getCompletedInterventions()).isEqualTo(3L);
        assertThat(report.getFrom()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(report.getTo()).isEqualTo(LocalDate.of(2024, 6, 30));
        assertThat(report.getDailyBreakdown()).hasSize(7);
        assertThat(report.getDailyBreakdown().get(6).getDate()).isEqualTo(reference);
    }

    @Test
    void getEarningsReport_returnsZeroWhenNoInterventions() {
        Mechanic mechanic = buildMechanic(2L);
        LocalDate reference = LocalDate.of(2024, 7, 1);

        when(mechanicRepository.findById(2L)).thenReturn(Optional.of(mechanic));
        when(interventionRepository.sumAmountByMechanicAndStatusAndDateBetween(
                eq(2L), eq(InterventionStatus.COMPLETED), any(), any()))
                .thenReturn(null);
        when(interventionRepository.countByMechanicIdAndStatusAndInterventionDateBetween(
                eq(2L), eq(InterventionStatus.COMPLETED), any(), any()))
                .thenReturn(0L);

        EarningsReportDTO report = earningsService.getEarningsReport(2L, reference);

        assertThat(report.getDailyEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getWeeklyEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getMonthlyEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getCompletedInterventions()).isZero();
    }

    @Test
    void getEarningsReport_throwsWhenMechanicNotFound() {
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> earningsService.getEarningsReport(99L, LocalDate.now()))
                .isInstanceOf(com.autopro.backend.exception.ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
