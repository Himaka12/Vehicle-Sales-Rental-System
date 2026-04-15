package com.example.Vehicle.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class PromotionUpsertDTO {
    private String title;
    private String description;
    private double discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private MultipartFile image;

    private boolean appliesToAllVehicles;
    private String targetBrand;
    private String targetModel;
    private String targetListingType;
    private String targetFuelType;
    private String targetVehicleCondition;
    private String targetCategory;

    private int priority;
    private boolean showOnInventoryBanner = true;
    private boolean showOnVehicleCard = true;
    private boolean showOnVehicleDetails = true;
    private String highlightLabel;
}
