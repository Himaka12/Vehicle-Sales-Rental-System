package com.example.Vehicle.dto;

import lombok.Data;

@Data
public class RentalBookingDTO {
    private Long id;
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
    private String vehicleStatus;
    private String customerName;
    private String customerEmail;

    private String startDate;
    private String startTime;
    private String endDate;
    private String endTime;

    private String status;
    private String paymentSlipUrl;
    private boolean premiumCustomer;
    private double totalFee;
    private String adminRespondedAt;

    // Refund system fields mapped for the frontend
    private String refundStatus;
    private String customerBankDetails;
    private String refundProofUrl;
    private boolean refundEligible;
    private boolean refundWindowStarted;
    private String refundPolicyMessage;
    private String refundClaimDeadline;
}
