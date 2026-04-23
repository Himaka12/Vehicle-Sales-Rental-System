package com.example.Vehicle.service;

import com.example.Vehicle.dto.CardDetailsDTO;
import com.example.Vehicle.dto.CustomerProfileDTO;

public interface CustomerService {
    CustomerProfileDTO getCustomerProfile(String email);
    CustomerProfileDTO updateCustomerProfile(String email, CustomerProfileDTO dto);
    CustomerProfileDTO completePremiumCheckout(String email, CardDetailsDTO cardDetails);
    void deleteCustomerAccount(String email);
    void changePassword(String email, String oldPassword, String newPassword);
    void removeCardDetails(String email);
}
