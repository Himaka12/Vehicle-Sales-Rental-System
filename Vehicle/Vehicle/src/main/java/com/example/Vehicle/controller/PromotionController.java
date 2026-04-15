package com.example.Vehicle.controller;

import com.example.Vehicle.dto.PromotionDTO;
import com.example.Vehicle.dto.PromotionUpsertDTO;
import com.example.Vehicle.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/add")
    public ResponseEntity<?> createPromotion(@ModelAttribute PromotionUpsertDTO request, Authentication authentication) {
        try {
            PromotionDTO promo = promotionService.createPromotion(
                    request,
                    authentication != null ? authentication.getName() : null
            );
            return ResponseEntity.ok(promo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create promotion: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<PromotionDTO>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PromotionDTO>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }

    @GetMapping("/showcase")
    public ResponseEntity<List<PromotionDTO>> getPromotionShowcase() {
        return ResponseEntity.ok(promotionService.getPromotionShowcase());
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<PromotionDTO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(promotionService.updatePromotionStatus(id, payload.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok("Promotion deleted successfully.");
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePromotion(
            @PathVariable Long id,
            @ModelAttribute PromotionUpsertDTO request,
            Authentication authentication) {
        try {
            PromotionDTO promo = promotionService.updatePromotion(
                    id,
                    request,
                    authentication != null ? authentication.getName() : null
            );
            return ResponseEntity.ok(promo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update promotion: " + e.getMessage());
        }
    }
}
