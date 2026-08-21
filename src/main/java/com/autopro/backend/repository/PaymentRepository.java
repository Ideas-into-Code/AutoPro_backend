package com.autopro.backend.repository;

import com.autopro.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderReference(String providerReference);

    List<Payment> findByPayerIdOrderByCreatedAtDesc(Long payerId);
}
