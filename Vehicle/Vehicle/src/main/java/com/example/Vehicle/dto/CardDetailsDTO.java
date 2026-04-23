package com.example.Vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CardDetailsDTO {
    @NotBlank(message = "Cardholder name is required.")
    @Size(max = 100, message = "Cardholder name must be 100 characters or fewer.")
    private String cardholderName;

    @NotBlank(message = "Card number is required.")
    @Pattern(
            regexp = "^(?:\\d{16}|\\d{4}(?: \\d{4}){3})$",
            message = "Card number must contain exactly 16 digits."
    )
    private String cardNumber;

    @NotBlank(message = "Expiry date is required.")
    @Pattern(
            regexp = "^(0[1-9]|1[0-2])/\\d{2}$",
            message = "Expiry date must be in MM/YY format."
    )
    private String expiry;

    @NotBlank(message = "CVV is required.")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits.")
    private String cvv;
}
