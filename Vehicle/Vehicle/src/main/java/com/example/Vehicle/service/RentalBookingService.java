package com.example.Vehicle.service;

import com.example.Vehicle.dto.RentalBookingDTO;
import com.example.Vehicle.entity.Refund;
import com.example.Vehicle.entity.RentalBooking;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.repository.RefundRepository;
import com.example.Vehicle.repository.RentalBookingRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.repository.VehicleRepository;
import com.example.Vehicle.util.StatusRules;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalBookingService {
    private static final long REFUND_WINDOW_HOURS_AFTER_ADMIN_RESPONSE = 24;
    private static final DateTimeFormatter REFUND_WINDOW_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RentalBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RefundRepository refundRepository;

    public boolean checkAvailability(Long vehicleId, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, Long excludeBookingId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        validateVehicleCanBeRented(vehicle);

        LocalDateTime newStart = LocalDateTime.of(startDate, startTime);
        LocalDateTime blockedUntil = validateAndBuildBlockedEnd(startDate, endDate, startTime, endTime);

        List<RentalBooking> overlaps = bookingRepository.findPotentialOverlaps(
                vehicleId,
                startDate,
                endDate,
                StatusRules.BOOKING_REJECTED
        );

        long overlappingActiveBookings = 0;
        for (RentalBooking existing : overlaps) {
            String existingStatus = StatusRules.normalizeBookingStatus(existing.getStatus());
            if (StatusRules.BOOKING_CANCELLED.equals(existingStatus)) {
                continue;
            }

            if (excludeBookingId != null && existing.getId().equals(excludeBookingId)) {
                continue;
            }

            LocalDateTime existingStart = LocalDateTime.of(existing.getStartDate(), existing.getStartTime());
            LocalDateTime existingBlockedUntil = LocalDateTime.of(existing.getEndDate(), existing.getEndTime()).plusHours(1);

            if (newStart.isBefore(existingBlockedUntil) && blockedUntil.isAfter(existingStart)) {
                overlappingActiveBookings++;
                if (overlappingActiveBookings >= vehicle.getQuantity()) {
                    return false;
                }
            }
        }

        return true;
    }

    public RentalBookingDTO createBooking(String email, Long vehicleId, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String slipUrl) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        validateVehicleCanBeRented(vehicle);

        if (!checkAvailability(vehicleId, startDate, endDate, startTime, endTime, null)) {
            throw new RuntimeException("This rental slot is fully booked, including the 1-hour cleaning buffer.");
        }

        RentalBooking booking = new RentalBooking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPaymentSlipUrl(slipUrl);

        return mapToDTO(bookingRepository.save(booking));
    }

    public List<RentalBookingDTO> getMyBookings(String email) {
        return bookingRepository.findByUserEmailOrderByIdDesc(email).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<RentalBookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public RentalBookingDTO updateBookingStatus(Long id, String status) {
        RentalBooking booking = bookingRepository.findById(id).orElseThrow(() -> new RuntimeException("Booking not found"));
        String currentStatus = StatusRules.normalizeBookingStatus(booking.getStatus());
        String normalizedStatus = StatusRules.normalizeBookingStatus(status);
        StatusRules.validateBookingAdminTransition(currentStatus, normalizedStatus);

        if (!currentStatus.equals(normalizedStatus)) {
            booking.setStatus(normalizedStatus);
            booking.setAdminRespondedAt(LocalDateTime.now());
        }

        return mapToDTO(bookingRepository.save(booking));
    }

    public RentalBookingDTO updateMyBooking(Long bookingId, String email, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String newSlipUrl) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        RentalBooking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this booking.");
        }

        if (!StatusRules.BOOKING_PENDING.equals(StatusRules.normalizeBookingStatus(booking.getStatus()))) {
            throw new RuntimeException("Business Rule Violation: You can only edit pending bookings.");
        }

        if (!checkAvailability(booking.getVehicle().getId(), startDate, endDate, startTime, endTime, booking.getId())) {
            throw new RuntimeException("This rental slot is fully booked, including the 1-hour cleaning buffer.");
        }

        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        if (newSlipUrl != null && !newSlipUrl.isEmpty()) {
            booking.setPaymentSlipUrl(newSlipUrl);
        }

        return mapToDTO(bookingRepository.save(booking));
    }

    public void deleteMyBooking(Long bookingId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        RentalBooking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this booking.");
        }

        throw new RuntimeException(
                "Bookings cannot be cancelled directly from the customer dashboard. "
                        + "Refund requests become available only for 24 hours after an admin approves or rejects the booking."
        );
    }

    private RentalBookingDTO mapToDTO(RentalBooking booking) {
        RentalBookingDTO dto = new RentalBookingDTO();
        dto.setId(booking.getId());
        dto.setVehicleId(booking.getVehicle().getId());
        dto.setVehicleName(booking.getVehicle().getBrand() + " " + booking.getVehicle().getModel());
        dto.setVehicleBrand(booking.getVehicle().getBrand());
        dto.setVehicleModel(booking.getVehicle().getModel());
        dto.setVehicleImageUrl(booking.getVehicle().getImage1());
        dto.setVehicleListingType(booking.getVehicle().getListingType());
        dto.setVehicleCondition(booking.getVehicle().getVehicleCondition());
        dto.setVehicleColor(booking.getVehicle().getColor());
        dto.setVehicleManufactureYear(booking.getVehicle().getManufactureYear());
        dto.setVehicleFuelType(booking.getVehicle().getFuelType());
        dto.setVehicleTransmission(booking.getVehicle().getTransmission());
        dto.setVehiclePrice(booking.getVehicle().getPrice());
        dto.setVehicleStatus(booking.getVehicle().getStatus());
        dto.setCustomerName(booking.getUser().getFullName());
        dto.setCustomerEmail(booking.getUser().getEmail());
        dto.setStartDate(booking.getStartDate() != null ? booking.getStartDate().toString() : "N/A");
        dto.setEndDate(booking.getEndDate() != null ? booking.getEndDate().toString() : "N/A");
        dto.setStartTime(booking.getStartTime() != null ? booking.getStartTime().toString() : "N/A");
        dto.setEndTime(booking.getEndTime() != null ? booking.getEndTime().toString() : "N/A");
        dto.setStatus(StatusRules.normalizeBookingStatus(booking.getStatus()));
        dto.setPaymentSlipUrl(booking.getPaymentSlipUrl());
        dto.setPremiumCustomer(booking.getUser().isPremium());
        dto.setAdminRespondedAt(booking.getAdminRespondedAt() != null ? booking.getAdminRespondedAt().toString() : null);

        Optional<Refund> refund = refundRepository.findByBookingId(booking.getId());
        LocalDateTime refundWindowStart = booking.getAdminRespondedAt();
        LocalDateTime refundDeadline = refundWindowStart != null
                ? refundWindowStart.plusHours(REFUND_WINDOW_HOURS_AFTER_ADMIN_RESPONSE)
                : null;
        boolean refundWindowStarted = hasRefundWindowStarted(dto.getStatus(), refundWindowStart);
        boolean refundEligible = false;

        if (refund.isPresent()) {
            dto.setRefundStatus(StatusRules.normalizeRefundStatus(refund.get().getStatus()));
            dto.setRefundProofUrl(refund.get().getRefundProofUrl());
        } else if (refundWindowStarted) {
            dto.setRefundStatus(StatusRules.REFUND_PENDING);
            refundEligible = !LocalDateTime.now().isAfter(refundDeadline);
        } else {
            dto.setRefundStatus("None");
        }

        dto.setRefundEligible(refundEligible);
        dto.setRefundWindowStarted(refundWindowStarted);
        dto.setRefundClaimDeadline(refundDeadline != null ? refundDeadline.toString() : null);
        dto.setRefundPolicyMessage(buildRefundPolicyMessage(dto.getStatus(), dto.getRefundStatus(), refundWindowStart, refundDeadline, refundEligible, refundWindowStarted));

        if (booking.getStartDate() != null && booking.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate()) + 1;
            double dailyRate = booking.getVehicle().getPrice();
            if (booking.getUser().isPremium()) {
                dailyRate = dailyRate * 0.90;
            }
            dto.setTotalFee(days * dailyRate);
        } else {
            dto.setTotalFee(0.0);
        }

        return dto;
    }

    @Transactional
    public void adminHardDeleteBooking(Long bookingId) {
        RentalBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        String currentStatus = StatusRules.normalizeBookingStatus(booking.getStatus());
        if (!StatusRules.BOOKING_CANCELLED.equals(currentStatus) && !StatusRules.BOOKING_REJECTED.equals(currentStatus)) {
            throw new RuntimeException("Business Rule Violation: You can only permanently delete cancelled or rejected bookings.");
        }

        refundRepository.deleteByBookingId(bookingId);
        bookingRepository.delete(booking);
    }

    private LocalDateTime validateAndBuildBlockedEnd(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
        LocalDateTime actualStart = LocalDateTime.of(startDate, startTime);
        LocalDateTime actualEnd = LocalDateTime.of(endDate, endTime);

        if (!actualEnd.isAfter(actualStart)) {
            throw new RuntimeException("End date and time must be after the start date and time.");
        }

        return actualEnd.plusHours(1);
    }

    private void validateVehicleCanBeRented(Vehicle vehicle) {
        if (!vehicle.isVisible()) {
            throw new RuntimeException("This listing is currently hidden and cannot accept new rentals.");
        }

        if (!"Rent".equalsIgnoreCase(vehicle.getListingType())) {
            throw new RuntimeException("This vehicle is not listed for rental.");
        }

        if (!"Available".equalsIgnoreCase(vehicle.getStatus())) {
            throw new RuntimeException("This vehicle is not currently available for rental.");
        }

        if (vehicle.getQuantity() < 1) {
            throw new RuntimeException("This vehicle is currently out of stock for rental.");
        }
    }

    private boolean hasRefundWindowStarted(String bookingStatus, LocalDateTime adminRespondedAt) {
        return adminRespondedAt != null
                && (StatusRules.BOOKING_APPROVED.equals(bookingStatus)
                || StatusRules.BOOKING_REJECTED.equals(bookingStatus)
                || StatusRules.BOOKING_CANCELLED.equals(bookingStatus));
    }

    private String buildRefundPolicyMessage(String bookingStatus, String refundStatus, LocalDateTime refundWindowStart, LocalDateTime refundDeadline, boolean refundEligible, boolean refundWindowStarted) {
        if (!StatusRules.REFUND_PENDING.equals(refundStatus) && !"None".equals(refundStatus)) {
            return "Your refund request is already in progress or completed.";
        }

        if (!refundWindowStarted) {
            if (StatusRules.BOOKING_PENDING.equals(bookingStatus)) {
                return "Your booking is waiting for admin review. The refund button appears only after the admin approves or rejects the booking, and it stays available for 24 hours.";
            }
            return "Refund eligibility cannot be verified because the admin response time is unavailable for this booking.";
        }

        String responseLabel = describeAdminResponse(bookingStatus);
        if (refundEligible) {
            return "Admin " + responseLabel + " this booking on " + formatRefundDateTime(refundWindowStart)
                    + ". Refund requests stay open until " + formatRefundDateTime(refundDeadline) + ".";
        }

        return "The 24-hour refund window started when admin " + responseLabel + " this booking on "
                + formatRefundDateTime(refundWindowStart) + " and closed on " + formatRefundDateTime(refundDeadline) + ".";
    }

    private String describeAdminResponse(String bookingStatus) {
        if (StatusRules.BOOKING_APPROVED.equals(bookingStatus)) {
            return "approved";
        }
        if (StatusRules.BOOKING_REJECTED.equals(bookingStatus)) {
            return "rejected";
        }
        return "responded to";
    }

    private String formatRefundDateTime(LocalDateTime value) {
        return value != null ? value.format(REFUND_WINDOW_FORMATTER) : "N/A";
    }
}
