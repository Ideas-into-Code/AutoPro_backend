package com.autopro.backend;

import com.autopro.backend.dto.mechanic.MechanicResponse;
import com.autopro.backend.dto.mechanic.UpdateMechanicProfileRequest;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.User;
import com.autopro.backend.entity.ValidationStatus;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.service.MechanicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MechanicServiceTest {

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MechanicService mechanicService;

    private User buildUser(Long id) {
        return User.builder().id(id).firstName("Jean").lastName("Dupont").email("jean" + id + "@example.com").build();
    }

    private Mechanic buildMechanic(Long id, User user) {
        return Mechanic.builder().id(id).user(user).specialization("Freins").isAvailable(true).build();
    }

    @Test
    void getById_returnsMechanic() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        when(mechanicRepository.findById(10L)).thenReturn(Optional.of(mechanic));

        MechanicResponse response = mechanicService.getById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSpecialization()).isEqualTo("Freins");
    }

    @Test
    void getById_throwsResourceNotFoundWhenMissing() {
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mechanicService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyProfile_createsProfileWhenNoneExists() {
        User user = buildUser(1L);
        Mechanic created = buildMechanic(20L, user);
        when(userRepository.findByEmail("jean1@example.com")).thenReturn(Optional.of(user));
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(mechanicRepository.save(any(Mechanic.class))).thenReturn(created);

        MechanicResponse response = mechanicService.getMyProfile("jean1@example.com");

        assertThat(response.getId()).isEqualTo(20L);
        verify(mechanicRepository).save(any(Mechanic.class));
    }

    @Test
    void updateMyProfile_updatesOnlyProvidedFields() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        mechanic.setExperienceYears(2);
        when(userRepository.findByEmail("jean1@example.com")).thenReturn(Optional.of(user));
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic));
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        UpdateMechanicProfileRequest request = new UpdateMechanicProfileRequest();
        request.setBio("Nouveau bio");
        request.setIsAvailable(false);
        request.setPhotoUrl("https://res.cloudinary.com/x/p.png");
        request.setOpeningHours("Lun-Ven 8h-18h");

        MechanicResponse response = mechanicService.updateMyProfile("jean1@example.com", request);

        assertThat(response.getBio()).isEqualTo("Nouveau bio");
        assertThat(response.getIsAvailable()).isFalse();
        assertThat(response.getPhotoUrl()).isEqualTo("https://res.cloudinary.com/x/p.png");
        assertThat(response.getOpeningHours()).isEqualTo("Lun-Ven 8h-18h");
        assertThat(mechanic.getExperienceYears()).isEqualTo(2); // non fourni : inchangé
        assertThat(mechanic.getSpecialization()).isEqualTo("Freins"); // non fourni : inchangé
    }

    @Test
    void updateMyProfile_blankPhotoUrlClearsIt() {
        User user = buildUser(2L);
        Mechanic mechanic = buildMechanic(20L, user);
        mechanic.setPhotoUrl("https://old/p.png");
        when(userRepository.findByEmail("jean2@example.com")).thenReturn(Optional.of(user));
        when(mechanicRepository.findByUserId(2L)).thenReturn(Optional.of(mechanic));
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        UpdateMechanicProfileRequest request = new UpdateMechanicProfileRequest();
        request.setPhotoUrl("");

        mechanicService.updateMyProfile("jean2@example.com", request);

        assertThat(mechanic.getPhotoUrl()).isNull();
    }

    @Test
    void updateAvailability_rejectsGoingOnlineWhenNotApproved() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        mechanic.setValidationStatus(ValidationStatus.PENDING);
        when(userRepository.findByEmail("jean1@example.com")).thenReturn(Optional.of(user));
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic));

        assertThatThrownBy(() -> mechanicService.updateAvailability("jean1@example.com", true))
                .isInstanceOf(SecurityException.class);
        verify(mechanicRepository, never()).save(any());
    }

    @Test
    void updateAvailability_allowsGoingOnlineWhenApproved() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        mechanic.setValidationStatus(ValidationStatus.APPROVED);
        mechanic.setIsAvailable(false);
        when(userRepository.findByEmail("jean1@example.com")).thenReturn(Optional.of(user));
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic));
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        MechanicResponse response = mechanicService.updateAvailability("jean1@example.com", true);

        assertThat(response.getIsAvailable()).isTrue();
    }

    @Test
    void updateAvailability_alwaysAllowsGoingOffline() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);
        mechanic.setValidationStatus(ValidationStatus.PENDING);
        when(userRepository.findByEmail("jean1@example.com")).thenReturn(Optional.of(user));
        when(mechanicRepository.findByUserId(1L)).thenReturn(Optional.of(mechanic));
        when(mechanicRepository.save(mechanic)).thenReturn(mechanic);

        MechanicResponse response = mechanicService.updateAvailability("jean1@example.com", false);

        assertThat(response.getIsAvailable()).isFalse();
    }

    @Test
    void findNearby_mapsProjectionsToResponsesWithDistance() {
        User user = buildUser(1L);
        Mechanic mechanic = buildMechanic(10L, user);

        MechanicRepository.MechanicDistanceProjection projection = new MechanicRepository.MechanicDistanceProjection() {
            public Long getId() { return 10L; }
            public Double getDistanceKm() { return 4.2; }
        };
        when(mechanicRepository.findNearby(14.7, -17.4, 20.0, null, true))
                .thenReturn(List.of(projection));
        when(mechanicRepository.findAllById(any())).thenReturn(List.of(mechanic));

        List<MechanicResponse> result = mechanicService.findNearby(14.7, -17.4, 20.0, null, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDistanceKm()).isEqualTo(4.2);
    }
}
