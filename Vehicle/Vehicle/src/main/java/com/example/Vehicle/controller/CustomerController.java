package com.example.Vehicle.controller;

import com.example.Vehicle.dto.CustomerProfileDTO;
import com.example.Vehicle.dto.CardDetailsDTO;
import com.example.Vehicle.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileDTO> getProfile(Authentication authentication) {
        return ResponseEntity.ok(customerService.getCustomerProfile(authentication.getName()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication authentication, @Valid @RequestBody com.example.Vehicle.dto.ChangePasswordDTO dto) {
        try {
            customerService.changePassword(authentication.getName(), dto.getOldPassword(), dto.getNewPassword());
            return ResponseEntity.ok("Password updated successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/remove-card")
    public ResponseEntity<String> removeCard(Authentication authentication) {
        customerService.removeCardDetails(authentication.getName());
        return ResponseEntity.ok("Card removed and premium subscription cancelled.");
    }

    @PutMapping("/update")
    public ResponseEntity<CustomerProfileDTO> updateProfile(Authentication authentication, @Valid @RequestBody CustomerProfileDTO dto) {
        return ResponseEntity.ok(customerService.updateCustomerProfile(authentication.getName(), dto));
    }

    @PostMapping("/add-card")
    public ResponseEntity<String> addCard(Authentication authentication, @Valid @RequestBody CardDetailsDTO cardData) {
        String response = customerService.addCardDetails(
                authentication.getName(), cardData.getCardNumber(), cardData.getExpiry(), cardData.getCvv()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(Authentication authentication) {
        customerService.deleteCustomerAccount(authentication.getName());
        return ResponseEntity.ok("Account deleted successfully");
    }
}
