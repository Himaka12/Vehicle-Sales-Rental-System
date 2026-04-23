package com.example.Vehicle.repository;

import com.example.Vehicle.entity.SalesInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesInquiryRepository extends JpaRepository<SalesInquiry, Long> {

    List<SalesInquiry> findAllByEmailOrderByIdDesc(String email);

    List<SalesInquiry> findAllByUserIdOrderByIdDesc(Long userId);

    void deleteByEmail(String email);

    // Checks if this customer already inquired about this specific vehicle!
    boolean existsByEmailAndVehicleId(String email, Long vehicleId);
    boolean existsByUserIdAndVehicleId(Long userId, Long vehicleId);

    long countByStatus(String status);

    void deleteByVehicleId(Long vehicleId);
}
