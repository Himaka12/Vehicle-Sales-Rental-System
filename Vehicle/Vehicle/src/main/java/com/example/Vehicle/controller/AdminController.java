package com.example.Vehicle.controller;

import com.example.Vehicle.dto.AdminStatsDTO;
import com.example.Vehicle.repository.RentalBookingRepository;
import com.example.Vehicle.repository.SalesInquiryRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final SalesInquiryRepository salesInquiryRepository;
    private final RentalBookingRepository rentalBookingRepository;

    //  The Real-Time Dashboard Analytics API
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getSystemStats() {
        AdminStatsDTO stats = new AdminStatsDTO();

        // Count everything natively from the database!
        stats.setTotalUsers(userRepository.count());
        stats.setTotalVehicles(vehicleRepository.count());
        stats.setPendingInquiries(salesInquiryRepository.countByStatus("Pending"));
        stats.setPendingRentals(rentalBookingRepository.countByStatus("Pending"));

        return ResponseEntity.ok(stats);
    }
}