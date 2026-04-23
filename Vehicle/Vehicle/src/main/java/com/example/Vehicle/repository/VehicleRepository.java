package com.example.Vehicle.repository;

import com.example.Vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByVisibleTrue();

    Optional<Vehicle> findByIdAndVisibleTrue(Long id);
}
