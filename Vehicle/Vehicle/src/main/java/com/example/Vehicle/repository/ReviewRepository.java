package com.example.Vehicle.repository;

import com.example.Vehicle.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Fetch all reviews for a specific car
    List<Review> findByVehicleIdOrderByReviewDateDesc(Long vehicleId);

    // Find reviews by user (Required for safe account deletion)
    List<Review> findByUserIdOrderByReviewDateDesc(Long userId);

    List<Review> findByVehicleIdAndUserIdOrderByIdAsc(Long vehicleId, Long userId);

    boolean existsByVehicleIdAndUserId(Long vehicleId, Long userId);

    // Fetch EVERY review for the Admin Dashboard, newest first!
    List<Review> findAllByOrderByReviewDateDesc();

    void deleteByVehicleId(Long vehicleId);
    void deleteByUserId(Long userId);
}
