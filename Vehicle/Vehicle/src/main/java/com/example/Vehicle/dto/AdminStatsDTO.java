package com.example.Vehicle.dto;

import lombok.Data;

@Data
public class AdminStatsDTO {
    private long totalUsers;
    private long totalVehicles;
    private long pendingInquiries;
    private long pendingRentals;
}