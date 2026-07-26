package com.autopro.backend.repository;

import com.autopro.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerId(Long ownerId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    Optional<Vehicle> findByVin(String vin);
}
