package com.example.Vehicle.service;

import com.example.Vehicle.dto.AdminReviewDTO;
import com.example.Vehicle.dto.ReviewAdminResponseDTO;
import com.example.Vehicle.dto.ReviewAiResult;
import com.example.Vehicle.dto.ReviewDTO;
import com.example.Vehicle.dto.ReviewModerationDTO;
import com.example.Vehicle.entity.RentalBooking;
import com.example.Vehicle.entity.Review;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.repository.RentalBookingRepository;
import com.example.Vehicle.repository.ReviewRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final String REVIEW_STATUS_ACTIVE = "Active";
    private static final String REVIEW_STATUS_REMOVED_BY_ADMIN = "RemovedByAdmin";

    private static final String AI_SENTIMENT_POSITIVE = "POSITIVE";
    private static final String AI_SENTIMENT_NEGATIVE = "NEGATIVE";
    private static final String AI_SENTIMENT_CRITICAL = "CRITICAL";

    private static final String ADMIN_ATTENTION_OPEN = "Open";
    private static final String ADMIN_ATTENTION_RESOLVED = "Resolved";
    private static final String ADMIN_ATTENTION_NOT_REQUIRED = "NotRequired";

    private static final String REPLY_SOURCE_AUTO_POSITIVE = "AutoPositivePool";
    private static final String REPLY_SOURCE_AUTO_NEGATIVE = "AutoNegativePool";
    private static final String REPLY_SOURCE_ADMIN_MANUAL = "AdminManual";

    private static final List<String> POSITIVE_REPLY_POOL = List.of(
            "Thank you for the kind review. We're glad this rental experience met your expectations and we appreciate you choosing K.D. Auto Traders.",
            "We appreciate your positive feedback. It means a lot to know the vehicle and service delivered a smooth experience for you.",
            "Thank you for sharing your experience. We're happy the rental went well and we look forward to serving you again.",
            "Your feedback is genuinely appreciated. We're pleased you had a strong rental experience and your support encourages the team."
    );

    private static final List<String> NEGATIVE_REPLY_POOL = List.of(
            "Thank you for the honest feedback. We're sorry the experience did not fully meet your expectations, and we will use your comments to improve our service.",
            "We appreciate you sharing this review. We're sorry that parts of the rental experience fell short, and your feedback has been noted carefully by our team.",
            "Thank you for letting us know where the experience could have been better. We take this seriously and will review it as part of our service improvements.",
            "We value your feedback and regret that the rental experience was not as smooth as it should have been. Your comments will help us improve future customer journeys."
    );

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RentalBookingRepository rentalBookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ReviewAiService reviewAiService;

    public ReviewDTO addReview(String email, ReviewDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        validateVisibleVehicleForCustomerInteraction(dto.getVehicleId(), "reviews");

        RentalBooking reviewableBooking = resolveReviewableBooking(dto.getVehicleId(), user.getId(), dto.getBookingId());

        Review review = new Review();
        review.setVehicleId(dto.getVehicleId());
        review.setUserId(user.getId());
        review.setBookingId(reviewableBooking.getId());
        review.setCustomerName(user.getFullName());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setReviewDate(LocalDate.now());
        review.setReviewStatus(REVIEW_STATUS_ACTIVE);
        review.setAdminRemovalReason(null);
        review.setAdminRemovalDate(null);
        resetAiAndReplyState(review);
        enrichReviewWithAiState(review);

        return mapToDTO(reviewRepository.save(review));
    }

    public List<ReviewDTO> getReviewsByVehicle(Long vehicleId) {
        if (vehicleRepository.findByIdAndVisibleTrue(vehicleId).isEmpty()) {
            return List.of();
        }

        return reviewRepository.findByVehicleIdOrderByReviewDateDesc(vehicleId)
                .stream()
                .filter(this::isPubliclyVisibleReview)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public boolean canUserReview(Long vehicleId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (vehicleRepository.findByIdAndVisibleTrue(vehicleId).isEmpty()) {
            return false;
        }
        return findNextReviewableBooking(vehicleId, user.getId()) != null;
    }

    public ReviewDTO updateReview(Long reviewId, String email, ReviewDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You can only edit your own reviews.");
        }
        if (isAdminRemovedReview(review)) {
            throw new RuntimeException("This review was removed by an administrator and can no longer be edited.");
        }

        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setReviewDate(LocalDate.now());
        resetAiAndReplyState(review);
        enrichReviewWithAiState(review);

        return mapToDTO(reviewRepository.save(review));
    }

    public void deleteReview(Long reviewId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!review.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You can only delete your own reviews.");
        }
        if (isAdminRemovedReview(review)) {
            throw new RuntimeException("This review was removed by an administrator and cannot be deleted from your moderation history.");
        }

        reviewRepository.delete(review);
    }

    public List<AdminReviewDTO> getAllReviewsForAdmin() {
        return reviewRepository.findAllByOrderByReviewDateDesc().stream()
                .map(review -> {
                    AdminReviewDTO adminDto = new AdminReviewDTO();
                    adminDto.setId(review.getId());
                    adminDto.setVehicleId(review.getVehicleId());
                    adminDto.setBookingId(review.getBookingId());
                    adminDto.setUserId(review.getUserId());
                    adminDto.setCustomerName(review.getCustomerName());
                    adminDto.setRating(review.getRating());
                    adminDto.setComment(review.getComment());
                    adminDto.setReviewDate(review.getReviewDate());
                    adminDto.setReviewStatus(normalizeReviewStatus(review.getReviewStatus()));
                    adminDto.setAdminRemovalReason(review.getAdminRemovalReason());
                    adminDto.setAdminRemovalDate(review.getAdminRemovalDate());
                    applyAiState(adminDto, review);

                    vehicleRepository.findById(review.getVehicleId()).ifPresentOrElse(
                            vehicle -> applyVehicleSummary(adminDto, vehicle),
                            () -> adminDto.setVehicleName("Unknown Vehicle (ID: " + review.getVehicleId() + ")")
                    );

                    return adminDto;
                })
                .collect(Collectors.toList());
    }

    public List<AdminReviewDTO> getMyReviews(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return reviewRepository.findByUserIdOrderByReviewDateDesc(user.getId()).stream()
                .map(review -> {
                    AdminReviewDTO dto = new AdminReviewDTO();
                    dto.setId(review.getId());
                    dto.setVehicleId(review.getVehicleId());
                    dto.setBookingId(review.getBookingId());
                    dto.setUserId(review.getUserId());
                    dto.setCustomerName(review.getCustomerName());
                    dto.setRating(review.getRating());
                    dto.setComment(review.getComment());
                    dto.setReviewDate(review.getReviewDate());
                    dto.setReviewStatus(normalizeReviewStatus(review.getReviewStatus()));
                    dto.setAdminRemovalReason(review.getAdminRemovalReason());
                    dto.setAdminRemovalDate(review.getAdminRemovalDate());
                    applyAiState(dto, review);

                    vehicleRepository.findById(review.getVehicleId()).ifPresentOrElse(
                            vehicle -> applyVehicleSummary(dto, vehicle),
                            () -> dto.setVehicleName("Unknown Vehicle (ID: " + review.getVehicleId() + ")")
                    );

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void adminDeleteReview(Long reviewId, ReviewModerationDTO moderationDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setReviewStatus(REVIEW_STATUS_REMOVED_BY_ADMIN);
        review.setAdminRemovalReason(moderationDTO.getReason().trim());
        review.setAdminRemovalDate(LocalDate.now());
        reviewRepository.save(review);
    }

    public void permanentlyDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (!isAdminRemovedReview(review)) {
            throw new RuntimeException("Only reviews that were already removed by admin can be deleted permanently.");
        }

        reviewRepository.delete(review);
    }

    public void respondToCriticalReview(Long reviewId, String adminEmail, ReviewAdminResponseDTO responseDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (isAdminRemovedReview(review)) {
            throw new RuntimeException("Removed reviews cannot receive a manual response.");
        }

        if (!Boolean.TRUE.equals(review.getRequiresAdminAttention())) {
            throw new RuntimeException("This review does not currently need manual admin follow-up.");
        }

        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        review.setBusinessReply(responseDTO.getResponseMessage().trim());
        review.setReplySource(REPLY_SOURCE_ADMIN_MANUAL);
        review.setBusinessReplyDate(LocalDate.now());
        review.setRequiresAdminAttention(false);
        review.setAdminAttentionStatus(ADMIN_ATTENTION_RESOLVED);
        review.setAdminResponseDate(LocalDate.now());
        review.setAdminResponderName(adminUser.getFullName());

        reviewRepository.save(review);
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setVehicleId(review.getVehicleId());
        dto.setUserId(review.getUserId());
        dto.setBookingId(review.getBookingId());
        dto.setCustomerName(review.getCustomerName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());
        dto.setReviewStatus(normalizeReviewStatus(review.getReviewStatus()));
        dto.setAdminRemovalReason(review.getAdminRemovalReason());
        dto.setAdminRemovalDate(review.getAdminRemovalDate());
        dto.setAiSentiment(normalizeAiSentiment(review.getAiSentiment()));
        dto.setAiReason(review.getAiReason());
        dto.setReplySource(review.getReplySource());
        dto.setBusinessReply(review.getBusinessReply());
        dto.setBusinessReplyDate(review.getBusinessReplyDate());
        dto.setRequiresAdminAttention(Boolean.TRUE.equals(review.getRequiresAdminAttention()));
        dto.setAdminAttentionStatus(normalizeAdminAttentionStatus(review.getAdminAttentionStatus(), review.getRequiresAdminAttention()));
        dto.setAdminAttentionReason(review.getAdminAttentionReason());
        dto.setAdminResponseDate(review.getAdminResponseDate());
        dto.setAdminResponderName(review.getAdminResponderName());
        return dto;
    }

    private boolean isPubliclyVisibleReview(Review review) {
        return REVIEW_STATUS_ACTIVE.equals(normalizeReviewStatus(review.getReviewStatus()));
    }

    private boolean isAdminRemovedReview(Review review) {
        return REVIEW_STATUS_REMOVED_BY_ADMIN.equals(normalizeReviewStatus(review.getReviewStatus()));
    }

    private String normalizeReviewStatus(String reviewStatus) {
        return (reviewStatus == null || reviewStatus.isBlank()) ? REVIEW_STATUS_ACTIVE : reviewStatus;
    }

    private String normalizeAiSentiment(String aiSentiment) {
        if (aiSentiment == null || aiSentiment.isBlank()) {
            return AI_SENTIMENT_NEGATIVE;
        }

        String normalized = aiSentiment.trim().toUpperCase(Locale.ROOT);
        if (AI_SENTIMENT_POSITIVE.equals(normalized) || AI_SENTIMENT_NEGATIVE.equals(normalized) || AI_SENTIMENT_CRITICAL.equals(normalized)) {
            return normalized;
        }
        return AI_SENTIMENT_NEGATIVE;
    }

    private String normalizeAdminAttentionStatus(String status, Boolean requiresAdminAttention) {
        if (status == null || status.isBlank()) {
            return Boolean.TRUE.equals(requiresAdminAttention) ? ADMIN_ATTENTION_OPEN : ADMIN_ATTENTION_NOT_REQUIRED;
        }
        return status;
    }

    private void enrichReviewWithAiState(Review review) {
        String vehicleName = vehicleRepository.findById(review.getVehicleId())
                .map(vehicle -> vehicle.getBrand() + " " + vehicle.getModel())
                .orElse("Unknown vehicle");

        ReviewAiResult aiResult = reviewAiService.analyzeReview(vehicleName, review.getRating(), review.getComment());

        review.setAiSentiment(normalizeAiSentiment(aiResult.getSentiment()));
        review.setAiReason(aiResult.getAiReason());
        review.setAdminAttentionReason(aiResult.getAdminAttentionReason());

        if (aiResult.isRequiresAdminAttention()) {
            review.setRequiresAdminAttention(true);
            review.setAdminAttentionStatus(ADMIN_ATTENTION_OPEN);
            review.setReplySource(null);
            review.setBusinessReply(null);
            review.setBusinessReplyDate(null);
            review.setAdminResponseDate(null);
            review.setAdminResponderName(null);
            return;
        }

        review.setRequiresAdminAttention(false);
        review.setAdminAttentionStatus(ADMIN_ATTENTION_NOT_REQUIRED);
        review.setAdminAttentionReason(null);
        review.setBusinessReplyDate(LocalDate.now());
        review.setAdminResponseDate(null);
        review.setAdminResponderName(null);

        if (AI_SENTIMENT_POSITIVE.equals(normalizeAiSentiment(aiResult.getSentiment()))) {
            review.setReplySource(REPLY_SOURCE_AUTO_POSITIVE);
            review.setBusinessReply(selectReplyFromPool(POSITIVE_REPLY_POOL, review));
        } else {
            review.setReplySource(REPLY_SOURCE_AUTO_NEGATIVE);
            review.setBusinessReply(selectReplyFromPool(NEGATIVE_REPLY_POOL, review));
        }
    }

    private void resetAiAndReplyState(Review review) {
        review.setAiSentiment(null);
        review.setAiReason(null);
        review.setReplySource(null);
        review.setBusinessReply(null);
        review.setBusinessReplyDate(null);
        review.setRequiresAdminAttention(false);
        review.setAdminAttentionStatus(ADMIN_ATTENTION_NOT_REQUIRED);
        review.setAdminAttentionReason(null);
        review.setAdminResponseDate(null);
        review.setAdminResponderName(null);
    }

    private String selectReplyFromPool(List<String> replies, Review review) {
        if (replies.isEmpty()) {
            return "";
        }

        int seed = (review.getComment() == null ? 0 : review.getComment().hashCode()) + review.getRating();
        int index = Math.floorMod(seed, replies.size());
        return replies.get(index);
    }

    private void applyAiState(AdminReviewDTO dto, Review review) {
        dto.setAiSentiment(normalizeAiSentiment(review.getAiSentiment()));
        dto.setAiReason(review.getAiReason());
        dto.setReplySource(review.getReplySource());
        dto.setBusinessReply(review.getBusinessReply());
        dto.setBusinessReplyDate(review.getBusinessReplyDate());
        dto.setRequiresAdminAttention(Boolean.TRUE.equals(review.getRequiresAdminAttention()));
        dto.setAdminAttentionStatus(normalizeAdminAttentionStatus(review.getAdminAttentionStatus(), review.getRequiresAdminAttention()));
        dto.setAdminAttentionReason(review.getAdminAttentionReason());
        dto.setAdminResponseDate(review.getAdminResponseDate());
        dto.setAdminResponderName(review.getAdminResponderName());
    }

    private void applyVehicleSummary(AdminReviewDTO dto, Vehicle vehicle) {
        dto.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());
        dto.setVehicleBrand(vehicle.getBrand());
        dto.setVehicleModel(vehicle.getModel());
        dto.setVehicleImageUrl(vehicle.getImage1());
        dto.setVehicleListingType(vehicle.getListingType());
        dto.setVehicleCondition(vehicle.getVehicleCondition());
        dto.setVehicleColor(vehicle.getColor());
        dto.setVehicleManufactureYear(vehicle.getManufactureYear());
        dto.setVehicleFuelType(vehicle.getFuelType());
        dto.setVehicleTransmission(vehicle.getTransmission());
        dto.setVehiclePrice(vehicle.getPrice());
        dto.setVehicleStatus(vehicle.getStatus());
    }

    private RentalBooking resolveReviewableBooking(Long vehicleId, Long userId, Long requestedBookingId) {
        List<RentalBooking> approvedBookings = rentalBookingRepository.findByVehicleIdAndUserIdAndStatusOrderByIdAsc(vehicleId, userId, "Approved");
        if (approvedBookings.isEmpty()) {
            throw new RuntimeException("Unauthorized: You must have an approved rental booking to review this vehicle.");
        }

        Set<Long> consumedBookingIds = buildConsumedReviewBookingIds(vehicleId, userId, approvedBookings);

        if (requestedBookingId != null) {
            RentalBooking selectedBooking = approvedBookings.stream()
                    .filter(booking -> requestedBookingId.equals(booking.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Selected booking is not an approved rental for this vehicle."));

            if (consumedBookingIds.contains(selectedBooking.getId())) {
                throw new RuntimeException("You have already reviewed this booking. Please edit or delete the existing review instead.");
            }
            return selectedBooking;
        }

        RentalBooking nextBooking = approvedBookings.stream()
                .filter(booking -> !consumedBookingIds.contains(booking.getId()))
                .findFirst()
                .orElse(null);

        if (nextBooking == null) {
            throw new RuntimeException("You have already reviewed all approved bookings for this vehicle.");
        }
        return nextBooking;
    }

    private RentalBooking findNextReviewableBooking(Long vehicleId, Long userId) {
        List<RentalBooking> approvedBookings = rentalBookingRepository.findByVehicleIdAndUserIdAndStatusOrderByIdAsc(vehicleId, userId, "Approved");
        if (approvedBookings.isEmpty()) {
            return null;
        }

        Set<Long> consumedBookingIds = buildConsumedReviewBookingIds(vehicleId, userId, approvedBookings);
        return approvedBookings.stream()
                .filter(booking -> !consumedBookingIds.contains(booking.getId()))
                .findFirst()
                .orElse(null);
    }

    private Set<Long> buildConsumedReviewBookingIds(Long vehicleId, Long userId, List<RentalBooking> approvedBookings) {
        List<Review> existingReviews = reviewRepository.findByVehicleIdAndUserIdOrderByIdAsc(vehicleId, userId);
        Set<Long> consumedBookingIds = existingReviews.stream()
                .map(Review::getBookingId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));

        long legacyReviewCount = existingReviews.stream()
                .filter(review -> review.getBookingId() == null)
                .count();

        for (RentalBooking booking : approvedBookings) {
            if (legacyReviewCount <= 0) {
                break;
            }
            if (consumedBookingIds.add(booking.getId())) {
                legacyReviewCount--;
            }
        }

        return consumedBookingIds;
    }

    private void validateVisibleVehicleForCustomerInteraction(Long vehicleId, String actionLabel) {
        if (vehicleRepository.findByIdAndVisibleTrue(vehicleId).isEmpty()) {
            throw new RuntimeException("This listing is currently hidden and cannot accept new " + actionLabel + ".");
        }
    }

}
