package com.example.Vehicle.controller;

import com.example.Vehicle.dto.RentalBookingDTO;
import com.example.Vehicle.service.RentalBookingService;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class RentalBookingController {

    private final RentalBookingService bookingService;

    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "excludeBookingId", required = false) Long excludeBookingId) {
        try {
            boolean isAvailable = bookingService.checkAvailability(
                    vehicleId,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    LocalTime.parse(startTime),
                    LocalTime.parse(endTime),
                    excludeBookingId
            );
            return ResponseEntity.ok(Map.of("available", isAvailable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rent")
    public ResponseEntity<?> createBooking(
            Authentication authentication,
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("paymentSlip") MultipartFile file) {

        try {
            UploadValidationUtil.validateImageFile(file, "Payment slip");
            String folder = "src/main/resources/static/uploads/slips/";
            Path uploadPath = Paths.get(folder);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = "/uploads/slips/" + fileName;

            RentalBookingDTO booking = bookingService.createBooking(
                    authentication.getName(), vehicleId,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    LocalTime.parse(startTime),
                    LocalTime.parse(endTime),
                    fileUrl
            );

            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Booking Failed: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMyBooking(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "paymentSlip", required = false) MultipartFile file) {

        try {
            String fileUrl = null;

            if (file != null && !file.isEmpty()) {
                UploadValidationUtil.validateImageFile(file, "Payment slip");
                String folder = "src/main/resources/static/uploads/slips/";
                Path uploadPath = Paths.get(folder);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                fileUrl = "/uploads/slips/" + fileName;
            }

            RentalBookingDTO updatedBooking = bookingService.updateMyBooking(
                    id, authentication.getName(),
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    LocalTime.parse(startTime),
                    LocalTime.parse(endTime),
                    fileUrl
            );
            return ResponseEntity.ok(updatedBooking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Update Failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMyBooking(Authentication authentication, @PathVariable Long id) {
        try {
            bookingService.deleteMyBooking(id, authentication.getName());
            return ResponseEntity.ok("Booking cancelled successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Cancellation Failed: " + e.getMessage());
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<RentalBookingDTO>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<RentalBookingDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, payload.get("status")));
    }

    @DeleteMapping("/admin-delete/{id}")
    public ResponseEntity<?> adminDeleteBooking(@PathVariable Long id) {
        try {
            bookingService.adminHardDeleteBooking(id);
            return ResponseEntity.ok("Booking permanently erased from the database.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Delete Failed: " + e.getMessage());
        }
    }
}
