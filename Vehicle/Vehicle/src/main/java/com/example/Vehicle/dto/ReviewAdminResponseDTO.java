package com.example.Vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewAdminResponseDTO {

    @NotBlank(message = "Admin response is required.")
    @Size(max = 1500, message = "Admin response must be 1500 characters or fewer.")
    private String responseMessage;
}
