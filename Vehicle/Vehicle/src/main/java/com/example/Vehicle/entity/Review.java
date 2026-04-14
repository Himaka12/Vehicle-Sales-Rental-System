package com.example.Vehicle.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vehicleId;
    private Long userId;

    private String customerName; // We store this so we don't have to join tables every time we load a review

    private int rating; // 1 to 5 stars

    @Column(length = 1000)
    private String comment;

    private LocalDate reviewDate;

    @Column(length = 40)
    private String reviewStatus;

    @Column(length = 1000)
    private String adminRemovalReason;

    private LocalDate adminRemovalDate;

    @Column(length = 30)
    private String aiSentiment;

    @Column(length = 500)
    private String aiReason;

    @Column(length = 30)
    private String replySource;

    @Column(length = 1500)
    private String businessReply;

    private LocalDate businessReplyDate;

    private Boolean requiresAdminAttention;

    @Column(length = 30)
    private String adminAttentionStatus;

    @Column(length = 500)
    private String adminAttentionReason;

    private LocalDate adminResponseDate;

    @Column(length = 150)
    private String adminResponderName;
}
