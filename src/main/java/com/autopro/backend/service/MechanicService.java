package com.autopro.backend.service;

import com.autopro.backend.dto.mechanic.MechanicResponse;
import com.autopro.backend.dto.mechanic.UpdateMechanicProfileRequest;
import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.User;
import com.autopro.backend.entity.ValidationStatus;
import com.autopro.backend.exception.ResourceNotFoundException;
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
@Transactional(readOnly = true)
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
                .orElseThrow(() -> new ResourceNotFoundException("Mécanicien introuvable: " + id));
        return toResponse(mechanic);
    }

    @Transactional
    public MechanicResponse getMyProfile(String email) {
        return toResponse(getOrCreateMechanicForCurrentUser(email));
    }

    /**
     * Bascule la disponibilité du mécanicien courant. Se rendre disponible
     * exige un profil validé par un administrateur.
     */
    @Transactional
    public MechanicResponse updateAvailability(String email, boolean available) {
        Mechanic mechanic = getOrCreateMechanicForCurrentUser(email);
        if (available && mechanic.getValidationStatus() != ValidationStatus.APPROVED) {
            throw new SecurityException(
                    "Votre profil doit être validé par un administrateur avant de recevoir des demandes");
        }
        mechanic.setIsAvailable(available);
        mechanicRepository.save(mechanic);
        return toResponse(mechanic);
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
            if (request.getIsAvailable()
                    && mechanic.getValidationStatus() != ValidationStatus.APPROVED) {
                throw new SecurityException(
                        "Votre profil doit être validé par un administrateur avant de recevoir des demandes");
            }
            mechanic.setIsAvailable(request.getIsAvailable());
        }
        if (request.getLatitude() != null) {
            mechanic.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            mechanic.setLongitude(request.getLongitude());
        }
        // Chaîne vide = « effacer » ; on distingue de `null` (« ne pas toucher »).
        if (request.getPhotoUrl() != null) {
            mechanic.setPhotoUrl(request.getPhotoUrl().isBlank() ? null : request.getPhotoUrl());
        }
        if (request.getOpeningHours() != null) {
            mechanic.setOpeningHours(request.getOpeningHours().isBlank() ? null : request.getOpeningHours());
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
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .specialization(mechanic.getSpecialization())
                .experienceYears(mechanic.getExperienceYears())
                .bio(mechanic.getBio())
                .photoUrl(mechanic.getPhotoUrl())
                .openingHours(mechanic.getOpeningHours())
                .isAvailable(mechanic.getIsAvailable())
                .validationStatus(mechanic.getValidationStatus() != null
                        ? mechanic.getValidationStatus().name() : null)
                .averageRating(mechanic.getAverageRating())
                .reviewCount(mechanic.getReviewCount())
                .latitude(mechanic.getLatitude())
                .longitude(mechanic.getLongitude())
                .distanceKm(distanceKm)
                .build();
    }
}