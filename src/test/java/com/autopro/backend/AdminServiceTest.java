package com.autopro.backend;

import com.autopro.backend.dto.admin.AdminStatsDTO;
import com.autopro.backend.dto.admin.MechanicDetailDTO;
import com.autopro.backend.entity.*;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private com.autopro.backend.service.NotificationService notificationService;

    @InjectMocks
    private AdminService adminService;

    private User buildUser(Long id, String role) {
        Role r = Role.builder().name(role).build();
        return User.builder()
                .id(id)
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean" + id + "@example.com")
                .password("hashed")
                .role(r)
                .build();
    }

    private Mechanic buildMechanic(Long id, ValidationStatus status) {
        User user = buildUser(id, "ROLE_MECHANIC");
        return Mechanic.builder()
                .id(id)
                .user(user)
                .specialization("Électricité")
                .experienceYears(3)
                .validationStatus(status)
                .build();
    }

    @Test
    void getSystemStats_returnsAggregatedCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(mechanicRepository.count()).thenReturn(4L);
        when(mechanicRepository.countByValidationStatus(ValidationStatus.PENDING)).thenReturn(2L);
        when(mechanicRepository.countByValidationStatus(ValidationStatus.APPROVED)).thenReturn(1L);
        when(mechanicRepository.countByValidationStatus(ValidationStatus.REJECTED)).thenReturn(1L);
        when(userRepository.countByIsActive(true)).thenReturn(8L);
        when(userRepository.countByIsActive(false)).thenReturn(2L);

        AdminStatsDTO stats = adminService.getSystemStats();

        assertThat(stats.getTotalUsers()).isEqualTo(10L);
        assertThat(stats.getTotalMechanics()).isEqualTo(4L);
        assertThat(stats.getPendingMechanics()).isEqualTo(2L);
        assertThat(stats.getApprovedMechanics()).isEqualTo(1L);
        assertThat(stats.getRejectedMechanics()).isEqualTo(1L);
        assertThat(stats.getActiveUsers()).isEqualTo(8L);
        assertThat(stats.getInactiveUsers()).isEqualTo(2L);
    }

    @Test
    void validateMechanic_approvesSuccessfully() {
        Mechanic mechanic = buildMechanic(1L, ValidationStatus.PENDING);
        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(mechanic));
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        MechanicDetailDTO result = adminService.validateMechanic(1L, true);

        assertThat(result.getValidationStatus()).isEqualTo("APPROVED");
        verify(mechanicRepository).save(mechanic);
    }

    @Test
    void validateMechanic_rejectsSuccessfully() {
        Mechanic mechanic = buildMechanic(2L, ValidationStatus.PENDING);
        when(mechanicRepository.findById(2L)).thenReturn(Optional.of(mechanic));
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        MechanicDetailDTO result = adminService.validateMechanic(2L, false);

        assertThat(result.getValidationStatus()).isEqualTo("REJECTED");
    }

    @Test
    void validateMechanic_throwsWhenNotFound() {
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.validateMechanic(99L, true))
                .isInstanceOf(com.autopro.backend.exception.ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getMechanicsByStatus_returnsPendingList() {
        Mechanic m1 = buildMechanic(1L, ValidationStatus.PENDING);
        Mechanic m2 = buildMechanic(2L, ValidationStatus.PENDING);
        when(mechanicRepository.findByValidationStatus(ValidationStatus.PENDING)).thenReturn(List.of(m1, m2));

        List<MechanicDetailDTO> result = adminService.getMechanicsByStatus(ValidationStatus.PENDING);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> "PENDING".equals(dto.getValidationStatus()));
    }
}
