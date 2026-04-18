package com.example.Vehicle.service;

import com.example.Vehicle.dto.RefundDTO;
import com.example.Vehicle.entity.Refund;
import com.example.Vehicle.entity.RentalBooking;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.repository.RefundRepository;
import com.example.Vehicle.repository.RentalBookingRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.util.StatusRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {
    private static final long REFUND_NOTICE_DAYS_BEFORE_START = 3;

    private final RefundRepository refundRepository;
    private final RentalBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public RefundDTO claimRefund(Long bookingId, String email, String bankName, String branchName, String accountNumber, String accountHolderName) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        RentalBooking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this booking.");
        }

        if (!StatusRules.BOOKING_CANCELLED.equals(StatusRules.normalizeBookingStatus(booking.getStatus()))) {
            throw new RuntimeException("Booking must be cancelled before claiming a refund.");
        }

        if (refundRepository.findByBookingId(bookingId).isPresent()) {
            throw new RuntimeException("A refund ticket already exists for this booking.");
        }

        validateRefundPolicyWindow(booking);

        validateBankDetails(bankName, branchName, accountNumber, accountHolderName);

        long days = java.time.temporal.ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate()) + 1;
        double dailyRate = booking.getVehicle().getPrice();
        if (user.isPremium()) {
            dailyRate = dailyRate * 0.90;
        }
        double totalAmount = days * dailyRate;

        Refund refund = new Refund();
        refund.setUser(user);
        refund.setBooking(booking);
        refund.setAmount(totalAmount);
        refund.setBankName(bankName.trim());
        refund.setBranchName(branchName.trim());
        refund.setAccountNumber(accountNumber.trim());
        refund.setAccountHolderName(accountHolderName.trim());
        refund.setStatus(StatusRules.REFUND_PROCESSING);

        return mapToDTO(refundRepository.save(refund));
    }

    public RefundDTO processRefund(Long refundId, String slipUrl) {
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new RuntimeException("Refund ticket not found"));

        if (!StatusRules.REFUND_PROCESSING.equals(StatusRules.normalizeRefundStatus(refund.getStatus()))) {
            throw new RuntimeException("Only processing refunds can be completed.");
        }

        refund.setRefundProofUrl(slipUrl);
        refund.setStatus(StatusRules.REFUND_COMPLETED);
        refund.setProcessedAt(LocalDateTime.now());
        return mapToDTO(refundRepository.save(refund));
    }

    public List<RefundDTO> getAllPendingRefunds() {
        return refundRepository.findByStatus(StatusRules.REFUND_PROCESSING)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RefundDTO> getAllRefunds() {
        return refundRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private RefundDTO mapToDTO(Refund refund) {
        RefundDTO dto = new RefundDTO();
        dto.setId(refund.getId());
        dto.setBookingId(refund.getBooking().getId());
        dto.setVehicleId(refund.getBooking().getVehicle().getId());
        dto.setVehicleName(refund.getBooking().getVehicle().getBrand() + " " + refund.getBooking().getVehicle().getModel());
        dto.setVehicleBrand(refund.getBooking().getVehicle().getBrand());
        dto.setVehicleModel(refund.getBooking().getVehicle().getModel());
        dto.setVehicleImageUrl(refund.getBooking().getVehicle().getImage1());
        dto.setVehicleListingType(refund.getBooking().getVehicle().getListingType());
        dto.setVehicleCondition(refund.getBooking().getVehicle().getVehicleCondition());
        dto.setVehicleColor(refund.getBooking().getVehicle().getColor());
        dto.setVehicleManufactureYear(refund.getBooking().getVehicle().getManufactureYear());
        dto.setVehicleFuelType(refund.getBooking().getVehicle().getFuelType());
        dto.setVehicleTransmission(refund.getBooking().getVehicle().getTransmission());
        dto.setVehiclePrice(refund.getBooking().getVehicle().getPrice());
        dto.setCustomerName(refund.getUser().getFullName());
        dto.setCustomerEmail(refund.getUser().getEmail());
        dto.setPremiumCustomer(refund.getUser().isPremium());
        dto.setAmount(refund.getAmount());
        dto.setBankName(refund.getBankName());
        dto.setBranchName(refund.getBranchName());
        dto.setAccountNumber(refund.getAccountNumber());
        dto.setAccountHolderName(refund.getAccountHolderName());
        dto.setStatus(StatusRules.normalizeRefundStatus(refund.getStatus()));
        dto.setRefundProofUrl(refund.getRefundProofUrl());
        dto.setCreatedAt(refund.getCreatedAt() != null ? refund.getCreatedAt().toString() : "N/A");
        dto.setProcessedAt(refund.getProcessedAt() != null ? refund.getProcessedAt().toString() : "N/A");
        return dto;
    }

    private void validateBankDetails(String bankName, String branchName, String accountNumber, String accountHolderName) {
        if (isBlank(bankName) || isBlank(branchName) || isBlank(accountNumber) || isBlank(accountHolderName)) {
            throw new RuntimeException("All bank account details are required for a refund claim.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateRefundPolicyWindow(RentalBooking booking) {
        if (booking.getStartDate() == null) {
            throw new RuntimeException("Refund policy could not be verified because the booking start date is missing.");
        }

        LocalDate deadline = booking.getStartDate().minusDays(REFUND_NOTICE_DAYS_BEFORE_START);
        if (LocalDate.now().isAfter(deadline)) {
            throw new RuntimeException("Refunds must be requested at least 3 days before the rental start date. The refund request window closed on " + deadline + ".");
        }
    }
}
