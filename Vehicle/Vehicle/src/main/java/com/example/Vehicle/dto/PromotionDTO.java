package com.example.Vehicle.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PromotionDTO {
    private Long id;
    private String title;
    private String description;
    private double discountPercentage;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private boolean appliesToAllVehicles;
    private String targetBrand;
    private String targetModel;
    private String targetListingType;
    private String targetFuelType;
    private String targetVehicleCondition;
    private String targetCategory;
    private int priority;
    private boolean showOnInventoryBanner;
    private boolean showOnVehicleCard;
    private boolean showOnVehicleDetails;
    private String highlightLabel;
    private Long createdByUserId;
    private Long updatedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String targetSummary;
}
