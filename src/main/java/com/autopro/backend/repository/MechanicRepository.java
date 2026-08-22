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

    /**
     * Recherche PostGIS : ST_DWithin s'appuie sur l'index GIST de "location" pour ne
     * calculer la distance exacte (ST_Distance) que sur les lignes déjà pré-filtrées
     * par la bounding box de l'index, au lieu de scanner toute la table (V16).
     */
    @Query(value =
        "SELECT id, distance_km AS distanceKm FROM ( " +
        "  SELECT m.id AS id, " +
        "    ST_Distance(m.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) / 1000.0 AS distance_km " +
        "  FROM mechanics m " +
        "  WHERE m.location IS NOT NULL " +
        "    AND ST_DWithin(m.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusKm * 1000) " +
        "    AND (CAST(:specialization AS varchar) IS NULL OR m.specialization ILIKE CONCAT('%', CAST(:specialization AS varchar), '%')) " +
        "    AND (:onlyAvailable = false OR m.is_available = true) " +
        ") sub " +
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
