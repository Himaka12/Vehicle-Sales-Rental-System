package com.example.Vehicle.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDTO {
    @NotBlank(message = "Full name is required.")
    @Size(max = 100, message = "Full name must be 100 characters or fewer.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Contact number is required.")
    @Size(max = 20, message = "Contact number must be 20 characters or fewer.")
    private String contactNumber;
}
