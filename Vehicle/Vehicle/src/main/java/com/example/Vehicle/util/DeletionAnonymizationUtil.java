package com.example.Vehicle.util;

public final class DeletionAnonymizationUtil {
    public static final String DELETED_USER_NAME = "Deleted User";
    public static final String DELETED_USER_ROLE = "DELETED_USER";

    private DeletionAnonymizationUtil() {
    }

    public static String buildDeletedUserEmail(Long userId) {
        return "deleted-user-" + userId + "@removed.local";
    }

    public static String buildDeletedInquiryEmail(Long userId, Long inquiryId) {
        return "deleted-inquiry-" + userId + "-" + inquiryId + "@removed.local";
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return "Removed";
        }

        String trimmed = accountNumber.trim();
        String lastFour = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return "****" + lastFour;
    }
}
