package com.example.Vehicle.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sales_inquiries")
@Data
public class SalesInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vehicleId; // Links to the specific car

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column
    private String preferredContactTime;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String status = "Pending"; // Pending, Resolved, or Rejected

    @Column(nullable = false)
    private String inquiryDate;
}
