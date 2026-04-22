package com.example.Vehicle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReviewDTO {
    private Long id;

    @NotNull(message = "Vehicle ID is required.")
    private Long vehicleId;
    private Long userId;
    private String customerName;

    @Min(value = 1, message = "Rating must be at least 1.")
    @Max(value = 5, message = "Rating cannot be more than 5.")
    private int rating;

    @NotBlank(message = "Review comment is required.")
    @Size(max = 1000, message = "Review comment must be 1000 characters or fewer.")
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
