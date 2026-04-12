package com.example.Vehicle.controller;

import com.example.Vehicle.dto.AuthResponseDTO;
import com.example.Vehicle.dto.LoginDTO;
import com.example.Vehicle.dto.RegisterDTO;
import com.example.Vehicle.dto.UpdateUserDTO;
import com.example.Vehicle.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Crucial for frontend communication
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            String response = authService.registerUser(registerDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            AuthResponseDTO response = authService.loginUser(loginDTO);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/add-subadmin")
    public ResponseEntity<String> addSubAdmin(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            String response = authService.registerSubAdmin(registerDTO, "MARKETING_MANAGER");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/subadmins")
    public ResponseEntity<List<Map<String, Object>>> getSubAdmins() {
        return new ResponseEntity<>(authService.getSubAdmins(), HttpStatus.OK);
    }

    @DeleteMapping("/delete-subadmin/{id}")
    public ResponseEntity<String> deleteSubAdmin(@PathVariable Long id) {
        try {
            authService.deleteSubAdmin(id);
            return new ResponseEntity<>("Sub-Admin deleted successfully!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Fetch ALL users for the Admin Hub
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return new ResponseEntity<>(authService.getAllUsers(), HttpStatus.OK);
    }

    // Delete ANY user (Customer, Sub-Admin, etc.)
    @DeleteMapping("/delete-user/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            authService.deleteUser(id);
            return new ResponseEntity<>("User deleted successfully!", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint for Super Admin to edit Sub-Admins
    @PutMapping("/update-subadmin/{id}")
    public ResponseEntity<?> updateSubAdmin(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        try {
            authService.updateSubAdmin(id, dto);
            return ResponseEntity.ok("Sub-Admin updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
