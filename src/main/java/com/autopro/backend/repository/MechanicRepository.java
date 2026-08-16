package com.autopro.backend.repository;

import com.autopro.backend.entity.Mechanic;
import com.autopro.backend.entity.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
