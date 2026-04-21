package com.example.Vehicle.controller;

import com.example.Vehicle.dto.AdminReviewDTO;
import com.example.Vehicle.dto.ReviewAdminResponseDTO;
import com.example.Vehicle.dto.ReviewDTO;
import com.example.Vehicle.dto.ReviewModerationDTO;
import com.example.Vehicle.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/add")
    public ResponseEntity<?> addReview(Authentication authentication, @Valid @RequestBody ReviewDTO dto) {
        try {
            String email = authentication.getName();
            ReviewDTO savedReview = reviewService.addReview(email, dto);
            return ResponseEntity.ok(savedReview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add review: " + e.getMessage());
        }
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<ReviewDTO>> getVehicleReviews(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(reviewService.getReviewsByVehicle(vehicleId));
    }

    @GetMapping("/can-review/{vehicleId}")
    public ResponseEntity<Boolean> canReview(Authentication authentication, @PathVariable Long vehicleId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(false);
        }
        try {
            boolean canReview = reviewService.canUserReview(vehicleId, authentication.getName());
            return ResponseEntity.ok(canReview);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    @PutMapping("/update/{reviewId}")
    public ResponseEntity<?> updateReview(Authentication authentication,
                                          @PathVariable Long reviewId,
                                          @Valid @RequestBody ReviewDTO dto) {
        try {
            ReviewDTO updatedReview = reviewService.updateReview(reviewId, authentication.getName(), dto);
            return ResponseEntity.ok(updatedReview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update review: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<?> deleteReview(Authentication authentication, @PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId, authentication.getName());
            return ResponseEntity.ok("Review deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete review: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<AdminReviewDTO>> getAllReviewsForAdmin() {
        return ResponseEntity.ok(reviewService.getAllReviewsForAdmin());
    }

    @PutMapping("/admin-delete/{reviewId}")
    public ResponseEntity<?> adminDeleteReview(@PathVariable Long reviewId,
                                               @Valid @RequestBody ReviewModerationDTO moderationDTO) {
        try {
            reviewService.adminDeleteReview(reviewId, moderationDTO);
            return ResponseEntity.ok("Review removed from public view and the moderation reason has been recorded for the customer.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to remove review: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin-purge/{reviewId}")
    public ResponseEntity<?> permanentlyDeleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.permanentlyDeleteReview(reviewId);
            return ResponseEntity.ok("Review permanently deleted from the database.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to permanently delete review: " + e.getMessage());
        }
    }

    @PutMapping("/admin-respond/{reviewId}")
    public ResponseEntity<?> adminRespondToCriticalReview(Authentication authentication,
                                                         @PathVariable Long reviewId,
                                                         @Valid @RequestBody ReviewAdminResponseDTO responseDTO) {
        try {
            reviewService.respondToCriticalReview(reviewId, authentication.getName(), responseDTO);
            return ResponseEntity.ok("Critical review response saved and the admin action has been completed.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to respond to critical review: " + e.getMessage());
        }
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<List<AdminReviewDTO>> getMyReviews(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(reviewService.getMyReviews(authentication.getName()));
    }
}
