package com.autopro.backend.repository;

import com.autopro.backend.entity.ServiceRequest;
import com.autopro.backend.entity.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<ServiceRequest> findByMechanicIdOrderByCreatedAtDesc(Long mechanicId);

    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);
}