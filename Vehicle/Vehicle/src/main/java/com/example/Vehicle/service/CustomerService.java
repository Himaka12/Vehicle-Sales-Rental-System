package com.example.Vehicle.service;

import com.example.Vehicle.dto.CustomerProfileDTO;

public interface CustomerService {
    CustomerProfileDTO getCustomerProfile(String email);
    CustomerProfileDTO updateCustomerProfile(String email, CustomerProfileDTO dto);
    String addCardDetails(String email, String cardNumber, String expiry, String cvv);
    void deleteCustomerAccount(String email);
    void changePassword(String email, String oldPassword, String newPassword);
    void removeCardDetails(String email);
}