package com.example.Vehicle.controller;

import com.example.Vehicle.dto.RefundDTO;
import com.example.Vehicle.service.RefundService;
import com.example.Vehicle.util.UploadValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/claim/{bookingId}")
    public ResponseEntity<?> claimRefund(
            Authentication authentication,
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> payload) {
        try {
            RefundDTO refund = refundService.claimRefund(
                    bookingId, authentication.getName(),
                    payload.get("bankName"),
                    payload.get("branchName"),
                    payload.get("accountNumber"),
                    payload.get("accountHolderName")
            );
            return ResponseEntity.ok(refund);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit refund claim: " + e.getMessage());
        }
    }

    @PostMapping("/process/{refundId}")
    public ResponseEntity<?> processRefund(
            @PathVariable Long refundId,
            @RequestParam("refundProof") MultipartFile file) {
        try {
            UploadValidationUtil.validateImageFile(file, "Refund proof");
            String folder = "src/main/resources/static/uploads/slips/";
            Path uploadPath = Paths.get(folder);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = System.currentTimeMillis() + "_refund_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(refundService.processRefund(refundId, "/uploads/slips/" + fileName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to process refund: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<RefundDTO>> getPendingRefunds() {
        return ResponseEntity.ok(refundService.getAllPendingRefunds());
    }

    @GetMapping("/all")
    public ResponseEntity<List<RefundDTO>> getAllRefunds() {
        return ResponseEntity.ok(refundService.getAllRefunds());
    }
}
