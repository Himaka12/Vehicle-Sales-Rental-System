package com.example.Vehicle.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the user who requested the refund
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Link to the specific cancelled booking
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private RentalBooking booking;

    // Financial Audit Details
    private double amount;

    // Dedicated Bank Details Columns
    private String bankName;
    private String branchName;
    private String accountNumber;
    private String accountHolderName;

    // Status: Pending, Processing, Completed, Rejected
    private String status = "Pending";

    // Proof of transfer uploaded by the Admin
    @Column(length = 500)
    private String refundProofUrl;

    // Timestamps for accurate financial auditing
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime processedAt;
}