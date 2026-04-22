package com.example.Vehicle.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    private double discountPercentage;

    private String imageUrl; // Path to the uploaded banner image

    private LocalDate startDate;
    private LocalDate endDate;

    private String status = "Active"; // Active, Expired, Disabled

    @Column(nullable = false)
    private boolean appliesToAllVehicles = false;

    private String targetBrand;
    private String targetModel;
    private String targetListingType;
    private String targetFuelType;
    private String targetVehicleCondition;
    private String targetCategory;

    @Column(nullable = false)
    private int priority = 0;

    @Column(nullable = false)
    private boolean showOnInventoryBanner = true;

    @Column(nullable = false)
    private boolean showOnVehicleCard = true;

    @Column(nullable = false)
    private boolean showOnVehicleDetails = true;

    private String highlightLabel;

    private Long createdByUserId;
    private Long updatedByUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
