package com.example.Vehicle.controller;

import com.example.Vehicle.dto.VehicleSubmitDTO;
import com.example.Vehicle.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final Validator validator;

    // Uses @RequestPart to accept 1 JSON string + Images (Bypasses Tomcat Limits!)
    @PostMapping(value = "/add", consumes = {"multipart/form-data"})
    public ResponseEntity<?> addVehicle(
            @RequestPart("vehicleData") String vehicleDataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            VehicleSubmitDTO submitDTO = mapper.readValue(vehicleDataJson, VehicleSubmitDTO.class);
            submitDTO.setImages(images);
            validateVehicleData(submitDTO);

            return new ResponseEntity<>(vehicleService.addVehicle(submitDTO), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Uses @RequestPart to safely accept JSON string + Images for updating
    @PutMapping(value = "/update/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateVehicle(
            @PathVariable Long id,
            @RequestPart("vehicleData") String vehicleDataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            VehicleSubmitDTO submitDTO = mapper.readValue(vehicleDataJson, VehicleSubmitDTO.class);
            submitDTO.setImages(images);

            return new ResponseEntity<>(vehicleService.updateVehicle(id, submitDTO), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllVehicles() {
        return new ResponseEntity<>(vehicleService.getAllVehicles(), HttpStatus.OK);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAdminVehicles() {
        return new ResponseEntity<>(vehicleService.getAdminVehicles(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable Long id) {
        return new ResponseEntity<>(vehicleService.getVehicleById(id), HttpStatus.OK);
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getAdminVehicleById(@PathVariable Long id) {
        return new ResponseEntity<>(vehicleService.getAdminVehicleById(id), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return new ResponseEntity<>("Vehicle deleted successfully", HttpStatus.OK);
    }

    @PutMapping("/visibility/{id}")
    public ResponseEntity<?> updateVehicleVisibility(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        Boolean visible = payload.get("visible");
        if (visible == null) {
            return new ResponseEntity<>("Vehicle visibility is required.", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(vehicleService.updateVehicleVisibility(id, visible), HttpStatus.OK);
    }

    private void validateVehicleData(VehicleSubmitDTO submitDTO) {
        Set<ConstraintViolation<VehicleSubmitDTO>> violations = validator.validate(submitDTO);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(", "));
            throw new RuntimeException(message);
        }
    }
}
