package com.example.Vehicle.service;

import com.example.Vehicle.entity.Refund;
import com.example.Vehicle.entity.RentalBooking;
import com.example.Vehicle.entity.SalesInquiry;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.repository.RefundRepository;
import com.example.Vehicle.repository.RentalBookingRepository;
import com.example.Vehicle.repository.ReviewRepository;
import com.example.Vehicle.repository.SalesInquiryRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.repository.WishlistRepository;
import com.example.Vehicle.util.DeletionAnonymizationUtil;
import com.example.Vehicle.util.StatusRules;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SalesInquiryRepository salesInquiryRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final RentalBookingRepository bookingRepository;
    private final RefundRepository refundRepository;

    @Transactional
    public void deleteCustomerAccountByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        deleteUserInternal(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        deleteUserInternal(user);
    }

    private void deleteUserInternal(User user) {
        if ("MAIN_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Security Alert: You cannot delete the Main Admin account!");
        }

        if (!user.isActive()) {
            throw new RuntimeException("This account has already been deleted or deactivated.");
        }

        if ("CUSTOMER".equals(user.getRole())) {
            cleanupCustomerData(user);
        }

        anonymizeAndDeactivateUser(user);
    }

    private void cleanupCustomerData(User user) {
        cancelActiveBookings(user);
        anonymizeInquiries(user);
        anonymizeCompletedRefundPayoutDetails(user);
        wishlistRepository.deleteByUser(user);
        reviewRepository.deleteByUserId(user.getId());
    }

    private void cancelActiveBookings(User user) {
        LocalDate today = LocalDate.now();
        List<RentalBooking> bookings = bookingRepository.findByUserIdOrderByIdDesc(user.getId());

        for (RentalBooking booking : bookings) {
            String status = StatusRules.normalizeBookingStatus(booking.getStatus());
            if (StatusRules.BOOKING_PENDING.equals(status)) {
                booking.setStatus(StatusRules.BOOKING_CANCELLED);
                continue;
            }

            if (StatusRules.BOOKING_APPROVED.equals(status)
                    && booking.getEndDate() != null
                    && !booking.getEndDate().isBefore(today)) {
                booking.setStatus(StatusRules.BOOKING_CANCELLED);
            }
        }
    }

    private void anonymizeInquiries(User user) {
        Map<Long, SalesInquiry> inquiriesById = new LinkedHashMap<>();
        salesInquiryRepository.findAllByUserIdOrderByIdDesc(user.getId())
                .forEach(inquiry -> inquiriesById.put(inquiry.getId(), inquiry));
        salesInquiryRepository.findAllByEmailOrderByIdDesc(user.getEmail())
                .forEach(inquiry -> inquiriesById.putIfAbsent(inquiry.getId(), inquiry));

        for (SalesInquiry inquiry : inquiriesById.values()) {
            inquiry.setUserId(user.getId());
            inquiry.setCustomerName(DeletionAnonymizationUtil.DELETED_USER_NAME);
            inquiry.setEmail(DeletionAnonymizationUtil.buildDeletedInquiryEmail(user.getId(), inquiry.getId()));
            inquiry.setPhone(DeletionAnonymizationUtil.DELETED_INQUIRY_PHONE);
        }
    }

    private void anonymizeCompletedRefundPayoutDetails(User user) {
        for (Refund refund : refundRepository.findByUserIdOrderByIdDesc(user.getId())) {
            maskCompletedRefundPayoutDetails(refund);
        }
    }

    public void maskCompletedRefundPayoutDetails(Refund refund) {
        if (!StatusRules.REFUND_COMPLETED.equals(StatusRules.normalizeRefundStatus(refund.getStatus()))) {
            return;
        }

        refund.setAccountNumber(DeletionAnonymizationUtil.maskAccountNumber(refund.getAccountNumber()));
        refund.setAccountHolderName(DeletionAnonymizationUtil.DELETED_USER_NAME);
    }

    private void anonymizeAndDeactivateUser(User user) {
        user.setFullName(DeletionAnonymizationUtil.DELETED_USER_NAME);
        user.setEmail(DeletionAnonymizationUtil.buildDeletedUserEmail(user.getId()));
        user.setContactNumber(null);
        user.setPassword(passwordEncoder.encode("disabled-account-" + user.getId()));
        user.setRole(DeletionAnonymizationUtil.DELETED_USER_ROLE);
        user.setCardNumber(null);
        user.setPremium(false);
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
