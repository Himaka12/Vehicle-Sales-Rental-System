package com.example.Vehicle.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminReviewDTO {
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
    private Long userId;
    private String customerName;
    private int rating;
    private String comment;
    private LocalDate reviewDate;
    private String reviewStatus;
    private String adminRemovalReason;
    private LocalDate adminRemovalDate;
    private String aiSentiment;
    private String aiReason;
    private String replySource;
    private String businessReply;
    private LocalDate businessReplyDate;
    private Boolean requiresAdminAttention;
    private String adminAttentionStatus;
    private String adminAttentionReason;
    private LocalDate adminResponseDate;
    private String adminResponderName;
}
