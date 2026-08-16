package com.autopro.backend.repository;

import com.autopro.backend.entity.Intervention;
import com.autopro.backend.entity.InterventionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {

    List<Intervention> findByMechanicIdAndInterventionDateBetween(
            Long mechanicId,
            LocalDateTime from,
            LocalDateTime to);

    @Query("""
            SELECT SUM(i.amount)
              FROM Intervention i
             WHERE i.mechanic.id = :mechanicId
               AND i.status      = :status
               AND i.interventionDate BETWEEN :from AND :to
            """)
    BigDecimal sumAmountByMechanicAndStatusAndDateBetween(
            @Param("mechanicId") Long mechanicId,
            @Param("status")     InterventionStatus status,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);

    long countByMechanicIdAndStatusAndInterventionDateBetween(
            Long mechanicId,
            InterventionStatus status,
            LocalDateTime from,
            LocalDateTime to);
}
