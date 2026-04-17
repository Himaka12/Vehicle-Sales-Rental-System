package com.example.Vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class VehicleSubmitDTO {
    @NotBlank(message = "Listing type is required.")
    private String listingType;

    @NotBlank(message = "Vehicle condition is required.")
    private String vehicleCondition;

    @NotBlank(message = "Brand is required.")
    @Size(max = 100, message = "Brand must be 100 characters or fewer.")
    private String brand;

    @NotBlank(message = "Model is required.")
    @Size(max = 100, message = "Model must be 100 characters or fewer.")
    private String model;

    @NotBlank(message = "Vehicle category is required.")
    @Size(max = 100, message = "Vehicle category must be 100 characters or fewer.")
    private String category;

    @NotNull(message = "Manufacture year is required.")
    @Min(value = 1886, message = "Manufacture year is invalid.")
    private Integer manufactureYear;

    @NotBlank(message = "Color is required.")
    @Size(max = 50, message = "Color must be 50 characters or fewer.")
    private String color;

    @NotNull(message = "Quantity is required.")
    @Min(value = 0, message = "Quantity cannot be negative.")
    private Integer quantity;

    @NotNull(message = "Mileage is required.")
    @Min(value = 0, message = "Mileage cannot be negative.")
    private Integer mileage;

    @NotBlank(message = "Engine capacity is required.")
    @Pattern(regexp = "^[1-9]\\d*$", message = "Engine capacity must be greater than 0.")
    @Size(max = 50, message = "Engine capacity must be 50 characters or fewer.")
    private String engineCapacity;

    @NotBlank(message = "Fuel type is required.")
    private String fuelType;

    @NotBlank(message = "Transmission is required.")
    private String transmission;

    @NotBlank(message = "Description is required.")
    @Size(max = 3000, message = "Description must be 3000 characters or fewer.")
    private String description;

    @NotNull(message = "Price is required.")
    @Min(value = 1, message = "Price must be greater than 0.")
    private Double price;

    @NotBlank(message = "Vehicle status is required.")
    private String status;
    private List<MultipartFile> images;
}
