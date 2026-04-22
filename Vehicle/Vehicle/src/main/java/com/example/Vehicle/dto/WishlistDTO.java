package com.example.Vehicle.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WishlistDTO {
    private Long wishlistId;
    private Long vehicleId;
    private String brand;
    private String model;
    private Double price;
    private String image1;
    private String listingType;
    private LocalDateTime addedDate;
}