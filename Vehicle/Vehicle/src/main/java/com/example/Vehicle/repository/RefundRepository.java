package com.example.Vehicle.repository;

import com.example.Vehicle.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByUserEmailOrderByIdDesc(String email);

    Optional<Refund> findByBookingId(Long bookingId);

    List<Refund> findByStatus(String status);

    boolean existsByBookingVehicleId(Long vehicleId);

    // 🔥 THE FIX: These two annotations give Spring permission to execute the Delete safely!
    @Transactional
    @Modifying
    void deleteByBookingId(Long bookingId);
}
