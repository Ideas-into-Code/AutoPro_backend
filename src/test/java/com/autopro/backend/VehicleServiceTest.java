package com.autopro.backend;

import com.autopro.backend.dto.vehicle.CreateVehicleRequest;
import com.autopro.backend.dto.vehicle.VehicleResponse;
import com.autopro.backend.entity.User;
import com.autopro.backend.entity.Vehicle;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.ServiceRequestRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.repository.VehicleRepository;
import com.autopro.backend.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks private VehicleService vehicleService;

    private User user(Long id) {
        return User.builder().id(id).email("user" + id + "@example.com").firstName("A").lastName("B").build();
    }

    private Vehicle vehicle(Long id, User owner) {
        return Vehicle.builder().id(id).owner(owner).brand("Toyota").model("Yaris")
                .year(2018).licensePlate("DK-1234-AA").build();
    }

    private CreateVehicleRequest req() {
        CreateVehicleRequest r = new CreateVehicleRequest();
        r.setBrand("Peugeot");
        r.setModel("208");
        r.setYear(2020);
        r.setLicensePlate("dk-9999-zz");
        return r;
    }

    @Test
    void create_savesNormalizedPlateForCurrentUser() {
        User owner = user(1L);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(owner));
        when(vehicleRepository.findByLicensePlate("DK9999ZZ")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponse response = vehicleService.create("user1@example.com", req());

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertThat(captor.getValue().getLicensePlate()).isEqualTo("DK9999ZZ");
        assertThat(captor.getValue().getOwner().getId()).isEqualTo(1L);
        assertThat(response.getBrand()).isEqualTo("Peugeot");
    }

    @Test
    void create_rejectsDuplicatePlate() {
        User owner = user(1L);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(owner));
        when(vehicleRepository.findByLicensePlate("DK9999ZZ"))
                .thenReturn(Optional.of(vehicle(9L, user(2L))));

        assertThatThrownBy(() -> vehicleService.create("user1@example.com", req()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void getMine_rejectsVehicleOfAnotherUser() {
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle(5L, user(2L))));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> vehicleService.getMine("user1@example.com", 5L))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getMine_throwsWhenMissing() {
        when(vehicleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getMine("user1@example.com", 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_blockedWhenVehicleHasActiveRequest() {
        User owner = user(1L);
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle(5L, owner)));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(owner));
        when(serviceRequestRepository.existsByVehicleIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> vehicleService.delete("user1@example.com", 5L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void delete_succeedsWhenNoActiveRequest() {
        User owner = user(1L);
        Vehicle v = vehicle(5L, owner);
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(v));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(owner));
        when(serviceRequestRepository.existsByVehicleIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(false);

        vehicleService.delete("user1@example.com", 5L);

        verify(vehicleRepository).delete(v);
    }

    @Test
    void listMine_returnsOwnerVehicles() {
        User owner = user(1L);
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(owner));
        when(vehicleRepository.findByOwnerId(1L)).thenReturn(List.of(vehicle(1L, owner), vehicle(2L, owner)));

        assertThat(vehicleService.listMine("user1@example.com")).hasSize(2);
    }

    @Test
    void update_rejectsPlateTakenByAnotherVehicle() {
        User owner = user(1L);
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle(5L, owner)));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(owner));
        when(vehicleRepository.findByLicensePlate("DK9999ZZ"))
                .thenReturn(Optional.of(vehicle(9L, owner)));

        assertThatThrownBy(() -> vehicleService.update("user1@example.com", 5L, req()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
