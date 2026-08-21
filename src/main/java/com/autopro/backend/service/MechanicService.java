package com.autopro.backend.service;

import com.autopro.backend.dto.mechanic.MechanicResponse;
import com.autopro.backend.dto.mechanic.UpdateMechanicProfileRequest;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.User;
import com.autopro.backend.repository.MechanicRepository;
import com.autopro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final UserRepository userRepository;

    public List<MechanicResponse> getAll(String specialization) {
        List<Mechanic> mechanics = (specialization == null || specialization.isBlank())
                ? mechanicRepository.findAll()
                : mechanicRepository.findBySpecializationContainingIgnoreCase(specialization);
        return mechanics.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MechanicResponse getById(Long id) {
        Mechanic mechanic = mechanicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mécanicien introuvable: " + id));
        return toResponse(mechanic);
    }

    public MechanicResponse getMyProfile(String email) {
        return toResponse(getOrCreateMechanicForCurrentUser(email));
    }

    @Transactional
    public MechanicResponse updateMyProfile(String email, UpdateMechanicProfileRequest request) {
        Mechanic mechanic = getOrCreateMechanicForCurrentUser(email);

        if (request.getSpecialization() != null) {
            mechanic.setSpecialization(request.getSpecialization());
        }
        if (request.getExperienceYears() != null) {
            mechanic.setExperienceYears(request.getExperienceYears());
        }
        if (request.getBio() != null) {
            mechanic.setBio(request.getBio());
        }
        if (request.getIsAvailable() != null) {
            mechanic.setIsAvailable(request.getIsAvailable());
        }
        if (request.getLatitude() != null) {
            mechanic.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            mechanic.setLongitude(request.getLongitude());
        }
        mechanicRepository.save(mechanic);
        return toResponse(mechanic);
    }

    public List<MechanicResponse> findNearby(double latitude, double longitude, double radiusKm,
                                              String specialization, boolean onlyAvailable) {
        List<MechanicRepository.MechanicDistanceProjection> projections =
                mechanicRepository.findNearby(latitude, longitude, radiusKm,
                        (specialization == null || specialization.isBlank()) ? null : specialization,
                        onlyAvailable);

        Map<Long, Double> distancesById = new LinkedHashMap<>();
        for (var p : projections) {
            distancesById.put(p.getId(), p.getDistanceKm());
        }

        List<Mechanic> mechanics = mechanicRepository.findAllById(distancesById.keySet());
        Map<Long, Mechanic> mechanicsById = mechanics.stream()
                .collect(Collectors.toMap(Mechanic::getId, m -> m));

        return distancesById.entrySet().stream()
                .map(entry -> toResponse(mechanicsById.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Mechanic getOrCreateMechanicForCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
        return mechanicRepository.findByUserId(user.getId())
                .orElseGet(() -> mechanicRepository.save(
                        Mechanic.builder().user(user).isAvailable(true).build()));
    }

    private MechanicResponse toResponse(Mechanic mechanic) {
        return toResponse(mechanic, null);
    }

    private MechanicResponse toResponse(Mechanic mechanic, Double distanceKm) {
        User user = mechanic.getUser();
        return MechanicResponse.builder()
                .id(mechanic.getId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .specialization(mechanic.getSpecialization())
                .experienceYears(mechanic.getExperienceYears())
                .bio(mechanic.getBio())
                .isAvailable(mechanic.getIsAvailable())
                .latitude(mechanic.getLatitude())
                .longitude(mechanic.getLongitude())
                .distanceKm(distanceKm)
                .build();
    }
}