package com.example.Vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewModerationDTO {

    @NotBlank(message = "Removal reason is required.")
    @Size(max = 1000, message = "Removal reason must be 1000 characters or fewer.")
    private String reason;
}
