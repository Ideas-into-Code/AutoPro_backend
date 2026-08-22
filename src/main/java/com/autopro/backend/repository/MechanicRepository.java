package com.autopro.backend.repository;

import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MechanicRepository extends JpaRepository<Mechanic, Long> {

    Optional<Mechanic> findByUserId(Long userId);

    List<Mechanic> findByIsAvailableTrue();

    List<Mechanic> findBySpecialization(String specialization);

    List<Mechanic> findByValidationStatus(ValidationStatus validationStatus);

    long countByValidationStatus(ValidationStatus validationStatus);

    List<Mechanic> findBySpecializationContainingIgnoreCase(String specialization);

    @Query(value =
        "SELECT id, distance_km AS distanceKm FROM ( " +
        "  SELECT m.id AS id, " +
        "    (6371 * acos(least(1.0, " +
        "        cos(radians(:lat)) * cos(radians(m.latitude)) * cos(radians(m.longitude) - radians(:lng)) " +
        "        + sin(radians(:lat)) * sin(radians(m.latitude)) " +
        "    ))) AS distance_km " +
        "  FROM mechanics m " +
        "  WHERE m.latitude IS NOT NULL AND m.longitude IS NOT NULL " +
        "    AND (CAST(:specialization AS varchar) IS NULL OR m.specialization ILIKE CONCAT('%', CAST(:specialization AS varchar), '%')) " +
        "    AND (:onlyAvailable = false OR m.is_available = true) " +
        ") sub " +
        "WHERE distance_km <= :radiusKm " +
        "ORDER BY distance_km ASC",
        nativeQuery = true)
    List<MechanicDistanceProjection> findNearby(
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            @Param("radiusKm") double radiusKm,
            @Param("specialization") String specialization,
            @Param("onlyAvailable") boolean onlyAvailable);

    interface MechanicDistanceProjection {
        Long getId();
        Double getDistanceKm();
    }
}
