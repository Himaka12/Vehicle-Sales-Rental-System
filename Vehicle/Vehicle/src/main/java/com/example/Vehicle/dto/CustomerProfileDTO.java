package com.example.Vehicle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerProfileDTO {
    @NotBlank(message = "Full name is required.")
    @Size(max = 100, message = "Full name must be 100 characters or fewer.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Contact number is required.")
    @Pattern(regexp = "^\\d{10}$", message = "Contact number must contain exactly 10 digits.")
    private String contactNumber;
    private boolean isPremium;
    private String cardNumber; // We will only send the last 4 digits to the frontend for security
}
