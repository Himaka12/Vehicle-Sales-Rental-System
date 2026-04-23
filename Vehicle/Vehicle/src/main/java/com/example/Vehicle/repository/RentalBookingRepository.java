package com.example.Vehicle.repository;

import com.example.Vehicle.entity.RentalBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentalBookingRepository extends JpaRepository<RentalBooking, Long> {

    // 🔥 UPGRADED: Finds potentially overlapping bookings based on Date ranges
    @Query("SELECT r FROM RentalBooking r WHERE r.vehicle.id = :vehicleId " +
            "AND r.status != :status " +
            "AND (r.startDate <= :endDate AND r.endDate >= :startDate)")
    List<RentalBooking> findPotentialOverlaps(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status
    );

    // Finds all bookings made by a specific customer
    List<RentalBooking> findByUserEmailOrderByIdDesc(String email);

    List<RentalBooking> findByUserIdOrderByIdDesc(Long userId);

    // Check if the user has an approved rental for this specific car!
    boolean existsByVehicleIdAndUserIdAndStatus(Long vehicleId, Long userId, String status);

    long countByStatus(String status);

    void deleteByVehicleId(Long vehicleId);
}
