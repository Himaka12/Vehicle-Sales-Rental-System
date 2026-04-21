package com.example.Vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CardDetailsDTO {
    @NotBlank(message = "Card number is required.")
    @Size(min = 12, max = 25, message = "Card number format is invalid.")
    private String cardNumber;

    @NotBlank(message = "Expiry date is required.")
    @Size(min = 4, max = 7, message = "Expiry date format is invalid.")
    private String expiry;

    @NotBlank(message = "CVV is required.")
    @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits.")
    private String cvv;
}
