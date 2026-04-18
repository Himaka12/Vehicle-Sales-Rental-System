package com.example.Vehicle.dto;

import lombok.Data;

@Data
public class RefundDTO {
    private Long id;
    private Long bookingId;
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
    private String customerEmail;
    private boolean premiumCustomer;

    private double amount;

    private String bankName;
    private String branchName;
    private String accountNumber;
    private String accountHolderName;

    private String status;
    private String refundProofUrl;

    private String createdAt;
    private String processedAt;
}
