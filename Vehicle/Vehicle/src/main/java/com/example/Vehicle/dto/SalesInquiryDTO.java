package com.example.Vehicle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SalesInquiryDTO {
    private Long id;

    @NotNull(message = "Vehicle ID is required.")
    private Long vehicleId;
    private String vehicleName;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleImageUrl;
    private String vehicleListingType;
    private String vehicleCondition;
    private String vehicleColor;
    private Integer vehicleManufactureYear;
    private String vehicleFuelType;
    private String vehicleTransmission;
    private Double vehiclePrice;
    private String customerName;
    private String email;

    @Size(max = 20, message = "Phone number must be 20 characters or fewer.")
    private String phone;

    @Size(max = 50, message = "Preferred contact time must be 50 characters or fewer.")
    private String preferredContactTime;

    @Size(max = 1000, message = "Message must be 1000 characters or fewer.")
    private String message;
    private String status;
    private String inquiryDate;
    private boolean isPremiumCustomer;
}
