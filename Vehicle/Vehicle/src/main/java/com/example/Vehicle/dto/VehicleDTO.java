package com.example.Vehicle.dto;

import lombok.Data;

@Data
public class VehicleDTO {
    private Long id;
    private String listingType;
    private String vehicleCondition;
    private String brand;
    private String model;
    private String category;
    private int manufactureYear;
    private String color;
    private String listedDate;
    private int quantity;
    private int mileage;
    private String engineCapacity;
    private String fuelType;
    private String transmission;
    private String description;
    private double price;
    private String status;
    private boolean visible;
    private String image1;
    private String image2;
    private String image3;
    private String image4;
    private String image5;
    private double effectivePrice;
    private AppliedPromotionDTO appliedPromotion;
}
