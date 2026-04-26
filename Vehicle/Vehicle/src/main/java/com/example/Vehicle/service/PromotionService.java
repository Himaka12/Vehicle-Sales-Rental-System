package com.example.Vehicle.service;

import com.example.Vehicle.dto.PromotionDTO;
import com.example.Vehicle.dto.PromotionUpsertDTO;
import com.example.Vehicle.entity.Promotion;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.repository.PromotionRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.util.StatusRules;
import com.example.Vehicle.util.UploadValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final PromotionMatchingService promotionMatchingService;
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/promotions/";

    public PromotionDTO createPromotion(PromotionUpsertDTO request, String currentUserEmail) {
        validatePromotionRequest(request, true);

        Promotion promo = new Promotion();
        applyRequestToPromotion(request, promo);
        promo.setStatus(determineAutomaticStatus(promo));
        promo.setCreatedByUserId(resolveUserId(currentUserEmail));
        promo.setUpdatedByUserId(resolveUserId(currentUserEmail));

        return mapToDTO(promotionRepository.save(promo));
    }

    public List<PromotionDTO> getAllPromotions() {
        syncPromotionStatuses();
        return promotionRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<PromotionDTO> getActivePromotions() {
        syncPromotionStatuses();
        LocalDate today = LocalDate.now();
        return promotionRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
                        StatusRules.PROMOTION_ACTIVE,
                        today,
                        today
                )
                .stream()
                .filter(promotion -> hasValidListingType(promotion.getTargetListingType()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<PromotionDTO> getPromotionShowcase() {
        syncPromotionStatuses();

        List<Promotion> liveBannerPromotions = promotionRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
                        StatusRules.PROMOTION_ACTIVE,
                        LocalDate.now(),
                        LocalDate.now()
                )
                .stream()
                .filter(promotion -> hasValidListingType(promotion.getTargetListingType()))
                .filter(Promotion::isShowOnInventoryBanner)
                .collect(Collectors.toList());

        if (!liveBannerPromotions.isEmpty()) {
            return liveBannerPromotions.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        return promotionRepository.findAll().stream()
                .filter(promotion -> StatusRules.PROMOTION_SCHEDULED.equals(promotion.getStatus()))
                .filter(Promotion::isShowOnInventoryBanner)
                .filter(promotion -> hasValidListingType(promotion.getTargetListingType()))
                .sorted((left, right) -> {
                    int startDateComparison = compareNullableDates(left.getStartDate(), right.getStartDate());
                    if (startDateComparison != 0) {
                        return startDateComparison;
                    }
                    return Long.compare(right.getId(), left.getId());
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    public PromotionDTO updatePromotionStatus(Long id, String status) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        StatusRules.validateManualPromotionStatus(status, promo.getEndDate());
        String normalizedStatus = StatusRules.normalizePromotionStatus(status);

        if (StatusRules.PROMOTION_ACTIVE.equals(normalizedStatus)) {
            promo.setStatus(determineAutomaticStatus(promo));
        } else {
            promo.setStatus(normalizedStatus);
        }
        return mapToDTO(promotionRepository.save(promo));
    }

    public PromotionDTO updatePromotion(Long id, PromotionUpsertDTO request, String currentUserEmail) {
        validatePromotionRequest(request, false);

        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        String currentStatus = promo.getStatus();
        applyRequestToPromotion(request, promo);
        promo.setStatus(resolveUpdatedStatus(currentStatus, promo));
        promo.setUpdatedByUserId(resolveUserId(currentUserEmail));

        return mapToDTO(promotionRepository.save(promo));
    }

    private void applyRequestToPromotion(PromotionUpsertDTO request, Promotion promo) {
        boolean appliesToAllVehicles = request.isAppliesToAllVehicles();

        promo.setTitle(request.getTitle().trim());
        promo.setDescription(request.getDescription().trim());
        promo.setDiscountPercentage(request.getDiscountPercentage());
        promo.setStartDate(request.getStartDate());
        promo.setEndDate(request.getEndDate());
        promo.setAppliesToAllVehicles(appliesToAllVehicles);
        promo.setTargetBrand(appliesToAllVehicles ? null : normalizeText(request.getTargetBrand()));
        promo.setTargetModel(appliesToAllVehicles ? null : normalizeText(request.getTargetModel()));
        promo.setTargetListingType(normalizeListingType(request.getTargetListingType()));
        promo.setTargetFuelType(null);
        promo.setTargetVehicleCondition(null);
        promo.setTargetCategory(null);
        promo.setPriority(0);
        promo.setShowOnInventoryBanner(request.isShowOnInventoryBanner());
        promo.setShowOnVehicleCard(request.isShowOnVehicleCard());
        promo.setShowOnVehicleDetails(request.isShowOnVehicleDetails());
        promo.setHighlightLabel(normalizeText(request.getHighlightLabel()));

        MultipartFile image = request.getImage();
        if (image != null && !image.isEmpty()) {
            promo.setImageUrl(saveImage(image));
        }
    }

    private void validatePromotionRequest(PromotionUpsertDTO request, boolean imageRequired) {
        if (request == null) {
            throw new RuntimeException("Promotion data is required.");
        }
        if (!hasText(request.getTitle())) {
            throw new RuntimeException("Promotion title is required.");
        }
        if (!hasText(request.getDescription())) {
            throw new RuntimeException("Promotion description is required.");
        }
        if (request.getDiscountPercentage() <= 0 || request.getDiscountPercentage() > 100) {
            throw new RuntimeException("Discount percentage must be between 1 and 100.");
        }
        if (!hasText(request.getTargetListingType())) {
            throw new RuntimeException("Choose whether this promotion applies to sale vehicles or rent vehicles.");
        }
        if (!isValidListingType(request.getTargetListingType())) {
            throw new RuntimeException("Promotion vehicle purpose must be Sale or Rent.");
        }

        validatePromotionDates(request.getStartDate(), request.getEndDate());

        if (imageRequired && (request.getImage() == null || request.getImage().isEmpty())) {
            throw new RuntimeException("Promotion banner image is required.");
        }

        if (!request.isAppliesToAllVehicles() && !hasText(request.getTargetBrand())) {
            throw new RuntimeException("Select 'Apply to all vehicles' or choose a vehicle brand for this promotion.");
        }

        if (hasText(request.getTargetModel()) && !hasText(request.getTargetBrand())) {
            throw new RuntimeException("Select a vehicle brand before choosing a model.");
        }
    }

    private String saveImage(MultipartFile file) {
        try {
            UploadValidationUtil.validateImageFile(file, "Promotion image");
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, file.getBytes());

            return "/uploads/promotions/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }

    private PromotionDTO mapToDTO(Promotion promotion) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(promotion.getId());
        dto.setTitle(promotion.getTitle());
        dto.setDescription(promotion.getDescription());
        dto.setDiscountPercentage(promotion.getDiscountPercentage());
        dto.setImageUrl(promotion.getImageUrl());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setStatus(StatusRules.normalizePromotionStatus(promotion.getStatus()));
        dto.setAppliesToAllVehicles(promotion.isAppliesToAllVehicles());
        dto.setTargetBrand(promotion.getTargetBrand());
        dto.setTargetModel(promotion.getTargetModel());
        dto.setTargetListingType(promotion.getTargetListingType());
        dto.setTargetFuelType(promotion.getTargetFuelType());
        dto.setTargetVehicleCondition(promotion.getTargetVehicleCondition());
        dto.setTargetCategory(promotion.getTargetCategory());
        dto.setPriority(promotion.getPriority());
        dto.setShowOnInventoryBanner(promotion.isShowOnInventoryBanner());
        dto.setShowOnVehicleCard(promotion.isShowOnVehicleCard());
        dto.setShowOnVehicleDetails(promotion.isShowOnVehicleDetails());
        dto.setHighlightLabel(promotion.getHighlightLabel());
        dto.setCreatedByUserId(promotion.getCreatedByUserId());
        dto.setUpdatedByUserId(promotion.getUpdatedByUserId());
        dto.setCreatedAt(promotion.getCreatedAt());
        dto.setUpdatedAt(promotion.getUpdatedAt());
        dto.setTargetSummary(promotionMatchingService.buildTargetSummary(promotion));
        return dto;
    }

    private void validatePromotionDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Promotion start and end dates are required.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Promotion start date must be today or a future date.");
        }
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Promotion end date cannot be before the start date.");
        }
    }

    private void syncPromotionStatuses() {
        LocalDate today = LocalDate.now();
        List<Promotion> promotions = promotionRepository.findAll();
        boolean changed = false;

        for (Promotion promotion : promotions) {
            String currentStatus = promotion.getStatus();
            String nextStatus;

            if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today)) {
                nextStatus = StatusRules.PROMOTION_EXPIRED;
            } else if (StatusRules.PROMOTION_DISABLED.equals(currentStatus)) {
                nextStatus = StatusRules.PROMOTION_DISABLED;
            } else {
                nextStatus = determineAutomaticStatus(promotion);
            }

            if (!nextStatus.equals(currentStatus)) {
                promotion.setStatus(nextStatus);
                changed = true;
            }
        }

        if (changed) {
            promotionRepository.saveAll(promotions);
        }
    }

    private String determineAutomaticStatus(Promotion promotion) {
        LocalDate today = LocalDate.now();
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(today)) {
            return StatusRules.PROMOTION_EXPIRED;
        }
        if (promotion.getStartDate() != null && promotion.getStartDate().isAfter(today)) {
            return StatusRules.PROMOTION_SCHEDULED;
        }
        return StatusRules.PROMOTION_ACTIVE;
    }

    private String resolveUpdatedStatus(String currentStatus, Promotion promotion) {
        if (StatusRules.PROMOTION_DISABLED.equals(currentStatus)
                && (promotion.getEndDate() == null || !promotion.getEndDate().isBefore(LocalDate.now()))) {
            return StatusRules.PROMOTION_DISABLED;
        }
        return determineAutomaticStatus(promotion);
    }

    private int compareNullableDates(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private Long resolveUserId(String email) {
        if (!hasText(email)) {
            return null;
        }

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalizeListingType(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("sale".equalsIgnoreCase(normalized)) {
            return "Sale";
        }
        if ("rent".equalsIgnoreCase(normalized)) {
            return "Rent";
        }
        return normalized;
    }

    private boolean isValidListingType(String value) {
        return "sale".equalsIgnoreCase(value) || "rent".equalsIgnoreCase(value);
    }

    private boolean hasValidListingType(String value) {
        return hasText(value) && isValidListingType(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
