package com.example.Vehicle.service;

import com.example.Vehicle.dto.CustomerProfileDTO;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountDeletionService accountDeletionService;

    @Override
    public CustomerProfileDTO getCustomerProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDTO(user);
    }

    @Override
    public CustomerProfileDTO updateCustomerProfile(String email, CustomerProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String trimmedEmail = dto.getEmail().trim();
        String trimmedFullName = dto.getFullName().trim();
        String trimmedContactNumber = dto.getContactNumber().trim();

        if (!trimmedEmail.equals(user.getEmail())) {
            if (userRepository.findByEmail(trimmedEmail).isPresent()) {
                throw new RuntimeException("This email is already in use by another account!");
            }
            user.setEmail(trimmedEmail);
        }

        user.setFullName(trimmedFullName);
        user.setContactNumber(trimmedContactNumber);

        return mapToDTO(userRepository.save(user));
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Incorrect old password!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public String addCardDetails(String email, String cardNumber, String expiry, String cvv) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        user.setCardNumber(maskCardNumber(cardNumber));
        user.setPremium(true);
        userRepository.save(user);
        return "Card added successfully. You are now a Premium Member!";
    }

    @Override
    public void deleteCustomerAccount(String email) {
        accountDeletionService.deleteCustomerAccountByEmail(email);
    }

    @Override
    public void removeCardDetails(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        user.setCardNumber(null);
        user.setPremium(false);
        userRepository.save(user);
    }

    private CustomerProfileDTO mapToDTO(User user) {
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setContactNumber(user.getContactNumber());
        dto.setPremium(user.isPremium());

        if (user.getCardNumber() != null && !user.getCardNumber().isBlank()) {
            if (user.getCardNumber().startsWith("****")) {
                dto.setCardNumber(user.getCardNumber());
            } else {
                String lastFour = user.getCardNumber().substring(user.getCardNumber().length() - 4);
                dto.setCardNumber("**** **** **** " + lastFour);
            }
        } else {
            dto.setCardNumber("No card added");
        }
        return dto;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null) {
            throw new RuntimeException("Card number is required.");
        }

        String digitsOnly = cardNumber.replaceAll("\\s+", "");
        if (!digitsOnly.matches("\\d{12,19}")) {
            throw new RuntimeException("Card number must contain between 12 and 19 digits.");
        }

        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
