package com.example.Vehicle.util;

import java.time.LocalDate;
import java.util.Set;

public final class StatusRules {

    public static final String BOOKING_PENDING = "Pending";
    public static final String BOOKING_APPROVED = "Approved";
    public static final String BOOKING_REJECTED = "Rejected";
    public static final String BOOKING_CANCELLED = "Cancelled";

    public static final String INQUIRY_PENDING = "Pending";
    public static final String INQUIRY_RESOLVED = "Resolved";
    public static final String INQUIRY_REJECTED = "Rejected";

    public static final String REFUND_PENDING = "Pending";
    public static final String REFUND_PROCESSING = "Processing";
    public static final String REFUND_COMPLETED = "Completed";
    public static final String REFUND_REJECTED = "Rejected";

    public static final String PROMOTION_ACTIVE = "Active";
    public static final String PROMOTION_SCHEDULED = "Scheduled";
    public static final String PROMOTION_DISABLED = "Disabled";
    public static final String PROMOTION_EXPIRED = "Expired";

    private static final Set<String> BOOKING_STATUSES = Set.of(
            BOOKING_PENDING, BOOKING_APPROVED, BOOKING_REJECTED, BOOKING_CANCELLED
    );
    private static final Set<String> INQUIRY_STATUSES = Set.of(
            INQUIRY_PENDING, INQUIRY_RESOLVED, INQUIRY_REJECTED
    );
    private static final Set<String> REFUND_STATUSES = Set.of(
            REFUND_PENDING, REFUND_PROCESSING, REFUND_COMPLETED, REFUND_REJECTED
    );
    private static final Set<String> PROMOTION_STATUSES = Set.of(
            PROMOTION_ACTIVE, PROMOTION_SCHEDULED, PROMOTION_DISABLED, PROMOTION_EXPIRED
    );

    private StatusRules() {
    }

    public static String normalizeBookingStatus(String status) {
        String normalized = normalizeRequiredValue(status, "Booking status");
        if (!BOOKING_STATUSES.contains(normalized)) {
            throw new RuntimeException("Invalid booking status.");
        }
        return normalized;
    }

    public static void validateBookingAdminTransition(String currentStatus, String nextStatus) {
        String current = normalizeBookingStatus(currentStatus);
        String next = normalizeBookingStatus(nextStatus);

        if (current.equals(next)) {
            return;
        }

        if (!BOOKING_PENDING.equals(current)) {
            throw new RuntimeException("Only pending bookings can be approved or rejected by admin.");
        }

        if (!BOOKING_APPROVED.equals(next) && !BOOKING_REJECTED.equals(next)) {
            throw new RuntimeException("Admin can only change a pending booking to Approved or Rejected.");
        }
    }

    public static String normalizeInquiryStatus(String status) {
        String normalized = normalizeRequiredValue(status, "Inquiry status");

        if ("Contacted".equalsIgnoreCase(normalized) || "Closed".equalsIgnoreCase(normalized)) {
            return INQUIRY_RESOLVED;
        }

        if (!INQUIRY_STATUSES.contains(normalized)) {
            throw new RuntimeException("Invalid inquiry status.");
        }
        return normalized;
    }

    public static void validateInquiryAdminTransition(String currentStatus, String nextStatus) {
        String current = normalizeInquiryStatus(currentStatus);
        String next = normalizeInquiryStatus(nextStatus);

        if (current.equals(next)) {
            return;
        }

        if (!INQUIRY_PENDING.equals(current)) {
            throw new RuntimeException("Only pending inquiries can be updated.");
        }

        if (!INQUIRY_RESOLVED.equals(next) && !INQUIRY_REJECTED.equals(next)) {
            throw new RuntimeException("Inquiry status must be Resolved or Rejected.");
        }
    }

    public static String normalizeRefundStatus(String status) {
        String normalized = normalizeRequiredValue(status, "Refund status");
        if (!REFUND_STATUSES.contains(normalized)) {
            throw new RuntimeException("Invalid refund status.");
        }
        return normalized;
    }

    public static String normalizePromotionStatus(String status) {
        String normalized = normalizeRequiredValue(status, "Promotion status");
        if (!PROMOTION_STATUSES.contains(normalized)) {
            throw new RuntimeException("Invalid promotion status.");
        }
        return normalized;
    }

    public static void validateManualPromotionStatus(String status, LocalDate endDate) {
        String normalized = normalizePromotionStatus(status);

        if (PROMOTION_EXPIRED.equals(normalized)) {
            throw new RuntimeException("Expired promotions are managed automatically by the system.");
        }

        if (PROMOTION_ACTIVE.equals(normalized) && endDate != null && endDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot activate a promotion that has already ended. Update the end date first.");
        }
    }

    private static String normalizeRequiredValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " is required.");
        }

        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }
}
