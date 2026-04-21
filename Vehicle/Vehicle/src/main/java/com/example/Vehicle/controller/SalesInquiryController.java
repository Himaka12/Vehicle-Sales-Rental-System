package com.example.Vehicle.controller;

import com.example.Vehicle.dto.SalesInquiryDTO;
import com.example.Vehicle.service.SalesInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class SalesInquiryController {

    private final SalesInquiryService inquiryService;

    @PostMapping("/add")
    public ResponseEntity<?> submitInquiry(Authentication authentication, @Valid @RequestBody SalesInquiryDTO dto) {
        try {
            SalesInquiryDTO savedInquiry = inquiryService.submitInquiry(authentication.getName(), dto);
            return ResponseEntity.ok(savedInquiry);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error submitting inquiry: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<SalesInquiryDTO>> getAllInquiries() {
        return ResponseEntity.ok(inquiryService.getAllInquiries());
    }

    @PutMapping("/update-status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String newStatus = payload.get("status");
            SalesInquiryDTO updatedInquiry = inquiryService.updateStatus(id, newStatus);
            return ResponseEntity.ok(updatedInquiry);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
        }
    }
    @GetMapping("/my-inquiries")
    public ResponseEntity<List<SalesInquiryDTO>> getMyInquiries(Authentication authentication) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(authentication.getName()));
    }

    @GetMapping("/check/{vehicleId}")
    public ResponseEntity<Boolean> checkInquiryStatus(Authentication authentication, @PathVariable Long vehicleId) {
        boolean exists = inquiryService.hasInquired(authentication.getName(), vehicleId);
        return ResponseEntity.ok(exists);
    }
}
