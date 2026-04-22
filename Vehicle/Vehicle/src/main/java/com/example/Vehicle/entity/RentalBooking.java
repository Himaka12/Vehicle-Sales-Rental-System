package com.example.Vehicle.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "rental_bookings")
public class RentalBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    private LocalDate startDate;
    private LocalTime startTime;

    private LocalDate endDate;
    private LocalTime endTime;

    private String status = "Pending"; // Pending, Approved, Rejected, Cancelled

    @Column(length = 500)
    private String paymentSlipUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}