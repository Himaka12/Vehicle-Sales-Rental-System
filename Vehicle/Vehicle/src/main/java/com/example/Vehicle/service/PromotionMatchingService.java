package com.example.Vehicle.service;

import com.example.Vehicle.dto.AppliedPromotionDTO;
import com.example.Vehicle.entity.Promotion;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.repository.PromotionRepository;
import com.example.Vehicle.util.StatusRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionMatchingService {

    private final PromotionRepository promotionRepository;

    public List<Promotion> getCurrentlyActivePromotions() {
        LocalDate today = LocalDate.now();
        return promotionRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
                        StatusRules.PROMOTION_ACTIVE,
                        today,
                        today
                )
                .stream()
                .filter(this::hasValidListingType)
                .toList();
    }

    public AppliedPromotionDTO resolveBestPromotion(Vehicle vehicle, List<Promotion> activePromotions) {
        if (vehicle == null || activePromotions == null || activePromotions.isEmpty()) {
            return null;
        }

        return activePromotions.stream()
                .filter(promotion -> matchesVehicle(promotion, vehicle))
                .sorted(
                        Comparator.comparingDouble(Promotion::getDiscountPercentage).reversed()
                                .thenComparing(Comparator.comparingLong(Promotion::getId).reversed())
                )
                .findFirst()
                .map(promotion -> mapAppliedPromotion(promotion, vehicle))
                .orElse(null);
    }

    public String buildTargetSummary(Promotion promotion) {
        if (promotion == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (promotion.isAppliesToAllVehicles() || !hasSpecificCriteria(promotion)) {
            return promotion.getTargetListingType() != null
                    ? "All " + promotion.getTargetListingType() + " vehicles"
                    : "All vehicles";
        }

        if (hasText(promotion.getTargetBrand())) parts.add("Brand: " + promotion.getTargetBrand());
        if (hasText(promotion.getTargetModel())) parts.add("Model: " + promotion.getTargetModel());
        if (hasText(promotion.getTargetListingType())) parts.add("Listing: " + promotion.getTargetListingType());
        if (hasText(promotion.getTargetVehicleCondition())) parts.add("Condition: " + promotion.getTargetVehicleCondition());

        return parts.isEmpty() ? "All vehicles" : String.join(" | ", parts);
    }

    private AppliedPromotionDTO mapAppliedPromotion(Promotion promotion, Vehicle vehicle) {
        AppliedPromotionDTO dto = new AppliedPromotionDTO();
        dto.setId(promotion.getId());
        dto.setTitle(promotion.getTitle());
        dto.setDescription(promotion.getDescription());
        dto.setImageUrl(promotion.getImageUrl());
        dto.setHighlightLabel(hasText(promotion.getHighlightLabel()) ? promotion.getHighlightLabel() : promotion.getTitle());
        dto.setDiscountPercentage(promotion.getDiscountPercentage());
        dto.setOriginalPrice(vehicle.getPrice());
        dto.setDiscountedPrice(calculateDiscountedPrice(vehicle.getPrice(), promotion.getDiscountPercentage()));
        dto.setEndDate(promotion.getEndDate());
        dto.setPriority(promotion.getPriority());
        dto.setShowOnInventoryBanner(promotion.isShowOnInventoryBanner());
        dto.setShowOnVehicleCard(promotion.isShowOnVehicleCard());
        dto.setShowOnVehicleDetails(promotion.isShowOnVehicleDetails());
        dto.setTargetSummary(buildTargetSummary(promotion));
        return dto;
    }

    private boolean matchesVehicle(Promotion promotion, Vehicle vehicle) {
        if (promotion == null || vehicle == null) {
            return false;
        }
        if (!hasValidListingType(promotion)) {
            return false;
        }

        if (promotion.isAppliesToAllVehicles() || !hasSpecificCriteria(promotion)) {
            return true;
        }

        return matchesText(promotion.getTargetBrand(), vehicle.getBrand())
                && matchesText(promotion.getTargetModel(), vehicle.getModel())
                && matchesText(promotion.getTargetListingType(), vehicle.getListingType())
                && matchesText(promotion.getTargetFuelType(), vehicle.getFuelType())
                && matchesText(promotion.getTargetVehicleCondition(), vehicle.getVehicleCondition())
                && matchesText(promotion.getTargetCategory(), vehicle.getCategory());
    }

    private boolean hasSpecificCriteria(Promotion promotion) {
        return hasText(promotion.getTargetBrand())
                || hasText(promotion.getTargetModel())
                || hasText(promotion.getTargetListingType())
                || hasText(promotion.getTargetFuelType())
                || hasText(promotion.getTargetVehicleCondition())
                || hasText(promotion.getTargetCategory());
    }

    private boolean matchesText(String target, String actual) {
        if (!hasText(target)) {
            return true;
        }
        return hasText(actual) && actual.trim().equalsIgnoreCase(target.trim());
    }

    private boolean hasValidListingType(Promotion promotion) {
        return promotion != null
                && hasText(promotion.getTargetListingType())
                && ("sale".equalsIgnoreCase(promotion.getTargetListingType())
                || "rent".equalsIgnoreCase(promotion.getTargetListingType()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double calculateDiscountedPrice(double originalPrice, double discountPercentage) {
        double discountAmount = originalPrice * (discountPercentage / 100.0);
        return Math.max(0, originalPrice - discountAmount);
    }
}
