package com.example.Vehicle.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AppliedPromotionDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String highlightLabel;
    private double discountPercentage;
    private double originalPrice;
    private double discountedPrice;
    private LocalDate endDate;
    private int priority;
    private boolean showOnInventoryBanner;
    private boolean showOnVehicleCard;
    private boolean showOnVehicleDetails;
    private String targetSummary;
}
