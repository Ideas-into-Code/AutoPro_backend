package com.autopro.backend.repository;

import com.autopro.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByMechanicIdOrderByCreatedAtDesc(Long mechanicId, Pageable pageable);

    boolean existsByMechanicIdAndReviewerId(Long mechanicId, Long reviewerId);

    long countByMechanicId(Long mechanicId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.mechanic.id = :mechanicId")
    Double averageRatingByMechanicId(@Param("mechanicId") Long mechanicId);
}
