package com.autopro.backend.service;

import com.autopro.backend.dto.vehicle.CreateVehicleRequest;
import com.autopro.backend.dto.vehicle.VehicleResponse;
import com.autopro.backend.entity.ServiceRequestStatus;
import com.autopro.backend.entity.User;
import com.autopro.backend.entity.Vehicle;
import com.autopro.backend.exception.ResourceNotFoundException;
import com.autopro.backend.repository.ServiceRequestRepository;
import com.autopro.backend.repository.UserRepository;
import com.autopro.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private static final Set<ServiceRequestStatus> ACTIVE_STATUSES = EnumSet.of(
            ServiceRequestStatus.PENDING,
            ServiceRequestStatus.ACCEPTED,
            ServiceRequestStatus.IN_PROGRESS);

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public List<VehicleResponse> listMine(String email) {
        return vehicleRepository.findByOwnerId(getUser(email).getId()).stream()
                .map(VehicleResponse::from)
                .collect(Collectors.toList());
    }

    public VehicleResponse getMine(String email, Long id) {
        return VehicleResponse.from(getOwned(email, id));
    }

    @Transactional
    public VehicleResponse create(String email, CreateVehicleRequest request) {
        User owner = getUser(email);
        assertPlateAvailable(request.getLicensePlate(), null);
        assertVinAvailable(request.getVin(), null);

        Vehicle vehicle = Vehicle.builder()
                .owner(owner)
                .brand(request.getBrand().trim())
                .model(request.getModel().trim())
                .year(request.getYear())
                .licensePlate(normalizePlate(request.getLicensePlate()))
                .vin(blankToNull(request.getVin()))
                .color(blankToNull(request.getColor()))
                .mileage(request.getMileage())
                .build();
        vehicleRepository.save(vehicle);
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public VehicleResponse update(String email, Long id, CreateVehicleRequest request) {
        Vehicle vehicle = getOwned(email, id);
        assertPlateAvailable(request.getLicensePlate(), id);
        assertVinAvailable(request.getVin(), id);

        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        vehicle.setLicensePlate(normalizePlate(request.getLicensePlate()));
        vehicle.setVin(blankToNull(request.getVin()));
        vehicle.setColor(blankToNull(request.getColor()));
        vehicle.setMileage(request.getMileage());
        vehicleRepository.save(vehicle);
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void delete(String email, Long id) {
        Vehicle vehicle = getOwned(email, id);
        if (serviceRequestRepository.existsByVehicleIdAndStatusIn(id, ACTIVE_STATUSES)) {
            throw new IllegalArgumentException(
                    "Ce véhicule est rattaché à une demande en cours et ne peut pas être supprimé");
        }
        vehicleRepository.delete(vehicle);
    }

    // --- helpers ---

    private Vehicle getOwned(String email, Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable: " + id));
        if (!vehicle.getOwner().getId().equals(getUser(email).getId())) {
            throw new SecurityException("Ce véhicule ne vous appartient pas");
        }
        return vehicle;
    }

    private void assertPlateAvailable(String plate, Long selfId) {
        vehicleRepository.findByLicensePlate(normalizePlate(plate))
                .filter(v -> !v.getId().equals(selfId))
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Cette immatriculation est déjà enregistrée");
                });
    }

    private void assertVinAvailable(String vin, Long selfId) {
        String normalized = blankToNull(vin);
        if (normalized == null) {
            return;
        }
        vehicleRepository.findByVin(normalized)
                .filter(v -> !v.getId().equals(selfId))
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Ce VIN est déjà enregistré");
                });
    }

    /**
     * Forme canonique d'une immatriculation : majuscules, sans espaces ni
     * tirets. « DK-4521-AB » et « dk 4521 ab » désignent la même plaque et ne
     * doivent pas pouvoir être enregistrées deux fois.
     */
    private static String normalizePlate(String plate) {
        return plate == null ? null : plate.replaceAll("[\\s-]+", "").toUpperCase();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
    }
}
