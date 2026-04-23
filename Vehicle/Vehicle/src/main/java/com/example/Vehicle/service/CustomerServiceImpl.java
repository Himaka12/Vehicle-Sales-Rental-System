package com.example.Vehicle.service;

import com.example.Vehicle.dto.CustomerProfileDTO;
import com.example.Vehicle.dto.CardDetailsDTO;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private static final String PREMIUM_LOCKED_MESSAGE =
            "Premium payment has already been approved. Card details cannot be edited or deleted after payment.";
    private static final DateTimeFormatter PREMIUM_PAYMENT_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyMMddHHmmss");

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
    public CustomerProfileDTO completePremiumCheckout(String email, CardDetailsDTO cardData) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isPremium()) {
            throw new RuntimeException(PREMIUM_LOCKED_MESSAGE);
        }

        validateCardholderName(cardData.getCardholderName());
        String normalizedCardNumber = normalizeCardNumber(cardData.getCardNumber());
        normalizeCvv(cardData.getCvv());
        validateFutureExpiry(cardData.getExpiry());

        user.setCardNumber(maskCardNumber(normalizedCardNumber));
        user.setPremium(true);
        user.setPremiumPaymentId(generatePremiumPaymentId());
        user.setPremiumActivatedAt(LocalDateTime.now());
        return mapToDTO(userRepository.save(user));
    }

    @Override
    public void deleteCustomerAccount(String email) {
        accountDeletionService.deleteCustomerAccountByEmail(email);
    }

    @Override
    public void removeCardDetails(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isPremium() || hasText(user.getPremiumPaymentId()) || user.getPremiumActivatedAt() != null) {
            throw new RuntimeException(PREMIUM_LOCKED_MESSAGE);
        }

        throw new RuntimeException("No approved premium payment exists for this account.");
    }

    private CustomerProfileDTO mapToDTO(User user) {
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setContactNumber(user.getContactNumber());
        dto.setPremium(user.isPremium());
        dto.setCardNumber(null);
        dto.setPremiumPaymentId(user.isPremium() ? resolvePremiumPaymentId(user) : null);
        dto.setPremiumActivatedAt(
                user.isPremium() && user.getPremiumActivatedAt() != null
                        ? user.getPremiumActivatedAt().toString()
                        : null
        );
        return dto;
    }

    private void validateCardholderName(String cardholderName) {
        if (!hasText(cardholderName)) {
            throw new RuntimeException("Cardholder name is required.");
        }

        String normalized = cardholderName.trim().replaceAll("\\s+", " ");
        if (normalized.length() < 2) {
            throw new RuntimeException("Cardholder name must contain at least 2 characters.");
        }
    }

    private String normalizeCardNumber(String cardNumber) {
        if (!hasText(cardNumber)) {
            throw new RuntimeException("Card number is required.");
        }

        String digitsOnly = cardNumber.replaceAll("\\s+", "");
        if (!digitsOnly.matches("\\d{16}")) {
            throw new RuntimeException("Card number must contain exactly 16 digits.");
        }

        return digitsOnly;
    }

    private String maskCardNumber(String normalizedCardNumber) {
        String lastFour = normalizedCardNumber.substring(normalizedCardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    private String normalizeCvv(String cvv) {
        if (!hasText(cvv)) {
            throw new RuntimeException("CVV is required.");
        }

        String digitsOnly = cvv.replaceAll("\\s+", "");
        if (!digitsOnly.matches("\\d{3,4}")) {
            throw new RuntimeException("CVV must be 3 or 4 digits.");
        }

        return digitsOnly;
    }

    private void validateFutureExpiry(String expiry) {
        if (!hasText(expiry) || !expiry.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            throw new RuntimeException("Expiry date must be in MM/YY format.");
        }

        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3, 5));
        YearMonth expiryMonth = YearMonth.of(year, month);

        if (expiryMonth.isBefore(YearMonth.now())) {
            throw new RuntimeException("Expiry date must be in the current month or a future month.");
        }
    }

    private String generatePremiumPaymentId() {
        String timestamp = LocalDateTime.now().format(PREMIUM_PAYMENT_ID_FORMATTER);
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "KDAT-" + timestamp + suffix;
    }

    private String resolvePremiumPaymentId(User user) {
        if (hasText(user.getPremiumPaymentId())) {
            return user.getPremiumPaymentId();
        }

        return "KDAT-LEGACY-" + String.format(Locale.ENGLISH, "%06d", user.getId());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
